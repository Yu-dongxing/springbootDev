/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/05/28
 */

package top.yuxs.springbootdev.modules.system.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson2.JSON;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import top.yuxs.springbootdev.core.annotation.AegisLog;
import top.yuxs.springbootdev.core.enums.BusinessType;
import top.yuxs.springbootdev.core.utils.IpUtils;
import top.yuxs.springbootdev.modules.system.entity.SysLog;
import top.yuxs.springbootdev.modules.system.event.AegisLogEvent;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 全面且无感知的接口请求日志拦截切面
 *
 * @author YuDongXing
 * @since 2026/05/28
 */
@Slf4j
@Aspect
@Component
public class AegisLogAspect {

    private final ApplicationEventPublisher eventPublisher;

    public AegisLogAspect(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * 拦截所有 RestController、Controller 注解类，
     * 以及 top.yuxs.springbootdev.modules 包下以 Controller 结尾的类的方法
     */
    @Pointcut("@within(org.springframework.web.bind.annotation.RestController) " +
            "|| @within(org.springframework.stereotype.Controller) " +
            "|| execution(* top.yuxs.springbootdev.modules..*Controller.*(..))")
    public void logPointcut() {
    }

    @Around("logPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        LocalDateTime requestTime = LocalDateTime.now();
        long startTime = System.currentTimeMillis();

        HttpServletRequest request = null;
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            request = attributes.getRequest();
        }

        // 1. 初始化日志记录
        SysLog sysLog = new SysLog();
        sysLog.setRequestTime(requestTime);

        // 2. 抓取请求基本信息 (IP, URL, Method)
        if (request != null) {
            sysLog.setIp(IpUtils.getClientIp(request));
            sysLog.setUrl(request.getRequestURI());
            sysLog.setMethod(request.getMethod());
        }

        // 3. 抓取当前登录人信息 (ID, 用户名, 角色)
        try {
            if (StpUtil.isLogin()) {
                sysLog.setUserId(StpUtil.getLoginIdAsLong());
                
                // 安全获取用户名：尝试从 SaSession 获取缓存的 username，获取不到则默认使用登录账号名
                String username = null;
                try {
                    if (StpUtil.getSession(false) != null) {
                        username = (String) StpUtil.getSession().get("username");
                    }
                } catch (Exception ignored) {}
                
                sysLog.setUsername(username != null ? username : StpUtil.getLoginId().toString());
                
                // 获取用户角色，拼接成逗号分割的字符串
                try {
                    sysLog.setUserRole(String.join(",", StpUtil.getRoleList()));
                } catch (Exception e) {
                    sysLog.setUserRole("default_role");
                }
            } else {
                // 未登录/访客状态
                sysLog.setUsername("访客");
                sysLog.setUserRole("visitor");
            }
        } catch (Exception e) {
            // 防御式：防止由于 Sa-Token 依赖或上下文未完成等原因阻碍核心业务流程
            sysLog.setUsername("访客/读取异常");
            sysLog.setUserRole("unknown");
        }

        // 4. 提取执行类和方法元数据
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String className = joinPoint.getTarget().getClass().getName();
        String methodName = method.getName();
        sysLog.setClassName(className);
        sysLog.setMethodName(methodName);

        // 5. 校验拦截开关注解
        AegisLog aegisLog = method.getAnnotation(AegisLog.class);
        boolean saveRequest = true;
        boolean saveResponse = true;

        if (aegisLog != null) {
            sysLog.setTitle(aegisLog.title());
            sysLog.setBusinessType(aegisLog.businessType().name());
            saveRequest = aegisLog.isSaveRequestData();
            saveResponse = aegisLog.isSaveResponseData();
        }

        // 6. 提取方法完整参数并序列化
        if (saveRequest) {
            sysLog.setParam(extractParams(joinPoint));
        }

        Object result = null;
        try {
            // 执行业务接口
            result = joinPoint.proceed();
            sysLog.setStatus(1); // 成功

            if (saveResponse && result != null) {
                String resultStr = JSON.toJSONString(result);
                // 长度防御，防止高容量出参撑满表 text 字段，必要时予以截断
                if (resultStr.length() > 5000) {
                    resultStr = resultStr.substring(0, 5000) + "... [内容过长已自动截断]";
                }
                sysLog.setResult(resultStr);
            }
            return result;
        } catch (Throwable e) {
            sysLog.setStatus(0); // 失败
            sysLog.setErrorMsg(getStackTrace(e));
            sysLog.setResult(e.getMessage());
            throw e; // 继续向上抛出，保证系统的全局异常处理器捕获，以及 Spring 数据库事务能够回滚
        } finally {
            // 7. 计算方法耗时
            long takeTime = System.currentTimeMillis() - startTime;
            sysLog.setTakeTime(takeTime);

            // 8. 兜底解析：当没有标 AegisLog 注解时，自动推导接口名称和操作类型
            deriveLogMetadata(sysLog, className, methodName);

            // 9. 发送解耦异步落库事件，利用基于 Java 21 虚拟线程池的监听器非阻塞存储
            try {
                eventPublisher.publishEvent(new AegisLogEvent(this, sysLog));
            } catch (Exception ex) {
                log.error("发布接口系统操作日志事件失败！", ex);
            }
        }
    }

    /**
     * 对未标注 AegisLog 注解的方法，通过类名、方法名和 HTTP Method 自动推导操作中文描述
     */
    private void deriveLogMetadata(SysLog sysLog, String className, String methodName) {
        // (1) 兜底推导接口中文注释名
        if (sysLog.getTitle() == null || sysLog.getTitle().isBlank()) {
            String classSimple = className.substring(className.lastIndexOf(".") + 1);
            String displayMethod = camelToChinese(methodName);
            sysLog.setTitle(classSimple + "#" + displayMethod);
        }

        // (2) 兜底推导操作类型 (INSERT/UPDATE/DELETE/SELECT/OTHER)
        if (sysLog.getBusinessType() == null || sysLog.getBusinessType().isBlank()) {
            String methodLower = methodName.toLowerCase();
            String httpMethod = sysLog.getMethod() != null ? sysLog.getMethod().toUpperCase() : "";

            if (methodLower.contains("add") || methodLower.contains("save") || methodLower.contains("insert") || methodLower.contains("create") || "POST".equals(httpMethod)) {
                sysLog.setBusinessType(BusinessType.INSERT.name());
            } else if (methodLower.contains("update") || methodLower.contains("modify") || methodLower.contains("edit") || methodLower.contains("change") || "PUT".equals(httpMethod)) {
                sysLog.setBusinessType(BusinessType.UPDATE.name());
            } else if (methodLower.contains("delete") || methodLower.contains("remove") || methodLower.contains("clear") || "DELETE".equals(httpMethod)) {
                sysLog.setBusinessType(BusinessType.DELETE.name());
            } else if (methodLower.contains("get") || methodLower.contains("query") || methodLower.contains("find") || methodLower.contains("list") || methodLower.contains("page") || methodLower.contains("select") || "GET".equals(httpMethod)) {
                sysLog.setBusinessType(BusinessType.SELECT.name());
            } else {
                sysLog.setBusinessType(BusinessType.OTHER.name());
            }
        }
    }

    /**
     * 解析方法中传入的全部入参，完美排除无法直接进行 JSON 序列化的底层类
     */
    private String extractParams(JoinPoint joinPoint) {
        Map<String, Object> params = new LinkedHashMap<>();
        
        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();

        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg == null) {
                continue;
            }

            // 拦截排除 Servlet 原生内置对象和多媒体文件，避免序列化失败与死循环
            if (arg instanceof HttpServletRequest || arg instanceof HttpServletResponse 
                    || arg instanceof MultipartFile || arg instanceof MultipartFile[]
                    || arg instanceof org.springframework.validation.BindingResult) {
                continue;
            }

            String name = (parameterNames != null && parameterNames.length > i) ? parameterNames[i] : "arg" + i;
            params.put(name, arg);
        }

        if (params.isEmpty()) {
            return "";
        }
        try {
            return JSON.toJSONString(params);
        } catch (Exception e) {
            return "[参数 JSON 序列化失败]";
        }
    }

    /**
     * 将常见代码驼峰命名转换或映射为友好的中文表示 (兜底翻译)
     */
    private String camelToChinese(String name) {
        String lower = name.toLowerCase();
        if (lower.contains("get") && lower.contains("all")) return "获取全部数据";
        if (lower.contains("list")) return "查询列表数据";
        if (lower.contains("page")) return "分页查询列表";
        if (lower.contains("get") || lower.contains("find") || lower.contains("query") || lower.contains("detail")) return "查询单条详情";
        if (lower.contains("save") || lower.contains("add") || lower.contains("insert") || lower.contains("create")) return "新增记录";
        if (lower.contains("update") || lower.contains("modify") || lower.contains("edit")) return "更新修改记录";
        if (lower.contains("delete") || lower.contains("remove") || lower.contains("clear")) return "物理/逻辑删除";
        if (lower.contains("upload")) return "上传文件/媒体";
        if (lower.contains("download") || lower.contains("export")) return "文件导出与下载";
        return name; // 无特征返回原样
    }

    /**
     * 将异常堆栈转为 String
     */
    private String getStackTrace(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.toString()).append("\n");
        int count = 0;
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
            if (++count > 25) { // 限制堆栈最大行数，减少数据库冗余
                sb.append("\t... [更多堆栈内容已省略]");
                break;
            }
        }
        return sb.toString();
    }
}

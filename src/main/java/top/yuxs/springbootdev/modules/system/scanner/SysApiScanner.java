/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/05/31
 */

package top.yuxs.springbootdev.modules.system.scanner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import top.yuxs.springbootdev.modules.system.entity.SysApi;
import top.yuxs.springbootdev.modules.system.service.SysApiService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 物理 API 接口资源全自动同步注册扫描器
 * 系统完全启动后，全自动捕获 Controller 路由注册至数据库
 *
 * @author YuDongXing
 * @since 2026/05/31
 */
@Component
@Slf4j
public class SysApiScanner implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Autowired
    private SysApiService sysApiService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info(">>>>>> 启动 Aegis-Boot 物理 API 全自动扫描注册任务...");
        
        try {
            Map<RequestMappingInfo, HandlerMethod> handlerMethods = requestMappingHandlerMapping.getHandlerMethods();
            List<SysApi> apiList = new ArrayList<>();

            for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
                RequestMappingInfo mappingInfo = entry.getKey();
                HandlerMethod handlerMethod = entry.getValue();

                // 过滤：只扫描业务模块包路径下的 Controller，防止将 Spring Boot 内置、监控端点接口混入
                String className = handlerMethod.getBeanType().getName();
                if (!className.startsWith("top.yuxs.springbootdev.modules")) {
                    continue;
                }

                // 1. 获取请求路由
                Set<String> patterns = null;
                if (mappingInfo.getPathPatternsCondition() != null) {
                    patterns = mappingInfo.getPathPatternsCondition().getPatternValues();
                } else if (mappingInfo.getPatternsCondition() != null) {
                    patterns = mappingInfo.getPatternsCondition().getPatterns();
                }

                if (patterns == null || patterns.isEmpty()) {
                    continue;
                }

                // 2. 获取 HTTP 请求方法 (GET, POST, PUT, DELETE)
                Set<RequestMethod> methods = mappingInfo.getMethodsCondition().getMethods();

                // 3. 提取所属业务模块 (如 system, file)
                String module = "other";
                String[] packageParts = className.split("\\.");
                for (int i = 0; i < packageParts.length; i++) {
                    if ("modules".equals(packageParts[i]) && i + 1 < packageParts.length) {
                        module = packageParts[i + 1];
                        break;
                    }
                }

                // 4. 为每个路由组合生成 SysApi 对象
                for (String path : patterns) {
                    // 核心设计：将 RESTful 动态参数路径如 /sys/user/{id} 转换为标准 Ant 匹配格式 /sys/user/*
                    String antPath = path.replaceAll("\\{[^}]+\\}", "*");

                    // 接口默认描述名称 (提取 类名:方法名，后续支持进一步丰富)
                    String controllerName = handlerMethod.getBeanType().getSimpleName();
                    String methodName = handlerMethod.getMethod().getName();
                    String friendlyApiName = controllerName + "." + methodName;

                    if (methods.isEmpty()) {
                        // 支持所有请求方式 (*)
                        SysApi sysApi = createApiEntity(friendlyApiName, antPath, "*", module);
                        apiList.add(sysApi);
                    } else {
                        for (RequestMethod m : methods) {
                            SysApi sysApi = createApiEntity(friendlyApiName, antPath, m.name().toUpperCase(), module);
                            apiList.add(sysApi);
                        }
                    }
                }
            }

            // 5. 调用 Service 批量持久化同步
            if (!apiList.isEmpty()) {
                sysApiService.syncApis(apiList);
                log.info(">>>>>> Aegis-Boot 物理 API 自动扫描注册完成，共同步 {} 个物理接口映射", apiList.size());
            } else {
                log.warn(">>>>>> 未扫描到任何符合条件的物理接口，请检查 Controller 配置！");
            }

        } catch (Exception e) {
            log.error(">>>>>> 物理 API 自动扫描注册任务发生异常:", e);
        }
    }

    private SysApi createApiEntity(String apiName, String path, String method, String module) {
        SysApi sysApi = new SysApi();
        sysApi.setApiName(apiName);
        sysApi.setPath(path);
        sysApi.setMethod(method);
        sysApi.setModule(module);
        return sysApi;
    }
}

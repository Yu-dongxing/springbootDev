package top.yuxs.springbootdev.controller.common;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.yuxs.springbootdev.common.Result;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 公共接口 -- 枚举 (自动化扫描版)
 */
@Slf4j
@RestController
@RequestMapping("/api/common")
public class CommonController {

    private final ApplicationContext applicationContext;

    // 缓存所有枚举的 JSON 结果
    private final Map<String, List<Map<String, Object>>> allEnumsCache = new ConcurrentHashMap<>();
    // 缓存枚举类名与 Class 的映射，方便单查
    private final Map<String, Class<?>> enumClassCache = new ConcurrentHashMap<>();

    public CommonController(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * 项目启动时执行一次，自动扫描项目包下所有的枚举并缓存
     */
    @PostConstruct
    public void initEnumCache() {
        // 1. 动态获取 Spring Boot 启动类所在的根包名
        String basePackage = getProjectBasePackage();
        log.info("开始扫描全局枚举，根包路径: {}", basePackage);

        // 2. 初始化扫描器，false 表示不使用默认的 @Component 过滤
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        // 优化：直接过滤出 Enum 类型，而不是 Object，极大提升扫描速度
        scanner.addIncludeFilter(new AssignableTypeFilter(Enum.class));

        Set<BeanDefinition> components = scanner.findCandidateComponents(basePackage);

        for (BeanDefinition component : components) {
            String className = component.getBeanClassName();
            try {
                Class<?> clazz = ClassUtils.forName(Objects.requireNonNull(className), ClassUtils.getDefaultClassLoader());
                if (clazz.isEnum()) {
                    String simpleName = clazz.getSimpleName();
                    // 驼峰命名作为 Key
                    String key = Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);

                    // 存入缓存
                    allEnumsCache.put(key, reflectEnumToList(clazz));
                    enumClassCache.put(simpleName, clazz);
                }
            } catch (Exception e) {
                log.error("解析枚举类 [{}] 失败", className, e);
            }
        }
        log.info("全局枚举扫描完成，共加载 {} 个枚举类", enumClassCache.size());
    }

    /**
     * 1. 一次性返回所有枚举类 (直接读缓存，毫秒级响应)
     */
    @GetMapping("/enums/all")
    public Result<Map<String, List<Map<String, Object>>>> getAllEnums() {
        return Result.success(allEnumsCache);
    }

    /**
     * 2. 指定类名返回枚举 (直接读缓存，毫秒级响应)
     * 示例: /api/common/enums/ResultCode
     */
    @GetMapping("/enums/{enumName}")
    public Result<?> getEnumByName(@PathVariable String enumName) {
        Class<?> clazz = enumClassCache.get(enumName);
        if (clazz != null) {
            try {
                return Result.success(reflectEnumToList(clazz));
            } catch (Exception e) {
                log.error("获取枚举失败: {}", enumName, e);
                return Result.error(2001, "获取枚举失败");
            }
        }
        return Result.error(2001, "未找到枚举类: " + enumName);
    }

    // --- 工具方法 ---

    /**
     * 动态获取项目启动类所在的根包
     */
    private String getProjectBasePackage() {
        Map<String, Object> annotatedBeans = applicationContext.getBeansWithAnnotation(SpringBootApplication.class);
        if (!annotatedBeans.isEmpty()) {
            // 获取带 @SpringBootApplication 的类所在的包名
            return annotatedBeans.values().iterator().next().getClass().getPackage().getName();
        }
        // 如果实在获取不到（极少情况），退化为当前 Controller 所在的顶级包（如 top.yuxs）
        String[] packages = this.getClass().getPackage().getName().split("\\.");
        return packages.length >= 2 ? packages[0] + "." + packages[1] : packages[0];
    }

    /**
     * 使用反射将枚举转为 List<Map>
     * 自动兼容 getCode/getValue 和 getMessage/getDesc/getDescription
     */
    private List<Map<String, Object>> reflectEnumToList(Class<?> clazz) throws Exception {
        List<Map<String, Object>> list = new ArrayList<>();
        Object[] constants = clazz.getEnumConstants();

        // 尝试寻找获取 "代码" 的方法
        Method getCodeMethod = findMethod(clazz, "getCode", "getValue");
        // 尝试寻找获取 "描述" 的方法
        Method getDescMethod = findMethod(clazz, "getMessage", "getDesc", "getDescription");

        for (Object obj : constants) {
            Map<String, Object> map = new HashMap<>();

            // 获取 code
            Object code = (getCodeMethod != null) ? getCodeMethod.invoke(obj) : ((Enum<?>)obj).name();
            // 获取 description
            Object desc = (getDescMethod != null) ? getDescMethod.invoke(obj) : obj.toString();

            map.put("value", code);
            map.put("description", desc);
            list.add(map);
        }
        return list;
    }

    private Method findMethod(Class<?> clazz, String... names) {
        for (String name : names) {
            try {
                return clazz.getMethod(name);
            } catch (NoSuchMethodException ignored) {}
        }
        return null;
    }
}
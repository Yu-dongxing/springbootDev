package top.yuxs.springbootdev.controller.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
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

/**
 * 公共接口 -- 枚举
 *
 */
@Slf4j
@RestController
@RequestMapping("/api/common")
public class CommonController {

    // 枚举类所在的根包名
    private final String ENUM_PACKAGE = "com.wzz.gspt.enums";

    /**
     * 1. 一次性返回所有枚举类
     * 返回的枚举类的类名为驼峰命名
     */
    @GetMapping("/enums/all")
    public Result<Map<String, List<Map<String, Object>>>> getAllEnums() {
        Map<String, List<Map<String, Object>>> result = new HashMap<>();

        // 扫描指定包下的所有类
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        // 接受所有类，后续判断是否为枚举
        scanner.addIncludeFilter(new AssignableTypeFilter(Object.class));

        Set<BeanDefinition> components = scanner.findCandidateComponents(ENUM_PACKAGE);
        for (BeanDefinition component : components) {
            try {
                Class<?> clazz = ClassUtils.forName(Objects.requireNonNull(component.getBeanClassName()), ClassUtils.getDefaultClassLoader());
                if (clazz.isEnum()) {
                    // 驼峰命名作为 Key
                    String key = Character.toLowerCase(clazz.getSimpleName().charAt(0)) + clazz.getSimpleName().substring(1);
                    result.put(key, reflectEnumToList(clazz));
                }
            } catch (Exception e) {
                log.error("扫描枚举 [{}] 失败", component.getBeanClassName(), e);
            }
        }
        return Result.success(result);
    }

    /**
     * 2. 指定类名返回枚举
     * 示例: /api/common/enums/ResultCode
     */
    @GetMapping("/enums/{enumName}")
    public Result<?> getEnumByName(@PathVariable String enumName) {
        try {
            Class<?> clazz = findEnumClass(enumName);
            if (clazz != null) {
                return Result.success(reflectEnumToList(clazz));
            }
            return Result.error(2001, "未找到枚举类: " + enumName);
        } catch (Exception e) {
            return Result.error(2001, "获取枚举失败");
        }
    }

    // --- 工具方法 ---

    /**
     * 使用反射将枚举转为 List<Map>
     * 自动兼容 getCode/getValue 和 getMessage/getDesc/getDesc
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

    private Class<?> findEnumClass(String simpleName) {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(Object.class));
        Set<BeanDefinition> components = scanner.findCandidateComponents(ENUM_PACKAGE);
        for (BeanDefinition component : components) {
            if (component.getBeanClassName().endsWith("." + simpleName)) {
                try {
                    return ClassUtils.forName(component.getBeanClassName(), ClassUtils.getDefaultClassLoader());
                } catch (ClassNotFoundException ignored) {}
            }
        }
        return null;
    }
}
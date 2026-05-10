/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/05/10
 */

package top.yuxs.springbootdev.core.db;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;
import top.yuxs.springbootdev.core.db.config.AegisDbProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 实体扫描器：负责扫描指定包下带有 @TableName 注解的类
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EntityScanner {

    private final AegisDbProperties properties;

    /**
     * 扫描实体类
     *
     * @return 实体类列表
     */
    public List<Class<?>> scanEntityClasses() {
        String basePackage = properties.getBasePackage();
        log.info("开始扫描实体类，基础包: {}", basePackage);
        List<Class<?>> entityClasses = new ArrayList<>();
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(TableName.class));
        
        for (var beanDef : scanner.findCandidateComponents(basePackage)) {
            try {
                Class<?> entityClass = Class.forName(beanDef.getBeanClassName());
                entityClasses.add(entityClass);
            } catch (ClassNotFoundException e) {
                log.error("未找到类: {}", beanDef.getBeanClassName(), e);
            }
        }
        return entityClasses;
    }
}

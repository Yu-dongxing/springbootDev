/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/04/11
 */

package top.yuxs.springbootdev.db;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import top.yuxs.springbootdev.annotation.db.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DatabaseInitService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${db.init.base-package}")
    private String basePackage;

    /**
     * 初始化入口
     */
    public void initDatabase() {
        log.info("开始数据库初始化，扫描包: {}", basePackage);
        List<Class<?>> entityClasses = scanEntityClasses(basePackage);

        // 第一阶段：创建或更新表结构、索引（不含外键）
        for (Class<?> entityClass : entityClasses) {
            TableName tableNameAnnotation = entityClass.getAnnotation(TableName.class);
            if (tableNameAnnotation != null) {
                String tableName = tableNameAnnotation.value();
                syncTableStructure(entityClass, tableName);
            }
        }

        // 第二阶段：统一处理外键约束（解决循环依赖）
        for (Class<?> entityClass : entityClasses) {
            TableName tableNameAnnotation = entityClass.getAnnotation(TableName.class);
            if (tableNameAnnotation != null) {
                syncForeignKeys(entityClass, tableNameAnnotation.value());
            }
        }
        log.info("数据库初始化完成");
    }

    private List<Class<?>> scanEntityClasses(String basePackage) {
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

    /**
     * 第一阶段：同步表结构（列新增/修改，索引同步）
     */
    private void syncTableStructure(Class<?> entityClass, String tableName) {
        if (!tableExists(tableName)) {
            createTable(entityClass, tableName);
        } else {
            updateTableColumnsAndIndexes(entityClass, tableName);
        }
    }

    private void createTable(Class<?> entityClass, String tableName) {
        try {
            StringBuilder sql = new StringBuilder("CREATE TABLE `").append(tableName).append("` (");
            List<Field> fields = getAllFields(entityClass);
            List<String> definitions = new ArrayList<>();
            boolean hasPrimaryKey = false;

            for (Field field : fields) {
                if (isIgnoreField(field)) continue;
                String columnDef = buildColumnDefinition(field);
                if (columnDef != null) {
                    definitions.add(columnDef);
                    if (field.isAnnotationPresent(TableId.class)) hasPrimaryKey = true;
                }
            }

            if (!hasPrimaryKey) {
                definitions.add(0, "`id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID'");
            }

            definitions.addAll(getIndexDefinitionsSQL(entityClass));
            sql.append(String.join(", ", definitions)).append(")");
            sql.append(getTableCommentSql(entityClass));
            sql.append(";");

            log.info("执行建表 SQL: {}", sql);
            jdbcTemplate.execute(sql.toString());
        } catch (Exception e) {
            log.error("创建表 {} 失败: {}", tableName, e.getMessage());
        }
    }

    private void updateTableColumnsAndIndexes(Class<?> entityClass, String tableName) {
        try {
            Map<String, String> existingColumnsMap = getExistingColumnsInfo(tableName);
            Set<String> existingIndexes = getExistingIndexNames(tableName);
            List<Field> fields = getAllFields(entityClass);

            for (Field field : fields) {
                if (isIgnoreField(field)) continue;

                String columnName = getColumnName(field);
                if (columnName == null) continue;

                String targetDefinition = buildColumnDefinition(field);
                String targetTypeOnly = getDataType(field).toLowerCase().replaceAll("\\s", "");

                if (!existingColumnsMap.containsKey(columnName.toLowerCase())) {
                    // 情况1：列不存在，执行新增
                    String sql = String.format("ALTER TABLE `%s` ADD COLUMN %s;", tableName, targetDefinition);
                    log.info("表 {} 新增列: {}", tableName, columnName);
                    jdbcTemplate.execute(sql);
                } else {
                    // 情况2：列存在，检查类型是否匹配（简单比对类型字符串）
                    String existingType = existingColumnsMap.get(columnName.toLowerCase()).toLowerCase().replaceAll("\\s", "");
                    // 如果定义中不包含现有类型（比如 varchar(255) vs varchar），则执行修改
                    if (!targetTypeOnly.equals(existingType)) {
                        String sql = String.format("ALTER TABLE `%s` MODIFY COLUMN %s;", tableName, targetDefinition);
                        log.info("表 {} 变更列类型: {} -> {}", tableName, existingType, targetTypeOnly);
                        jdbcTemplate.execute(sql);
                    }
                }
            }

            // 索引同步
            Index[] indexes = entityClass.getAnnotationsByType(Index.class);
            for (Index index : indexes) {
                if (!existingIndexes.contains(index.name().toLowerCase())) {
                    String sql = "ALTER TABLE `" + tableName + "` ADD " + buildIndexDefinition(index) + ";";
                    jdbcTemplate.execute(sql);
                    log.info("表 {} 新增索引: {}", tableName, index.name());
                }
            }
        } catch (Exception e) {
            log.error("更新表 {} 失败: {}", tableName, e.getMessage());
        }
    }

    /**
     * 第二阶段：同步外键
     */
    private void syncForeignKeys(Class<?> entityClass, String tableName) {
        try {
            Set<String> existingFks = getExistingForeignKeyNames(tableName);
            ForeignKey[] foreignKeys = entityClass.getAnnotationsByType(ForeignKey.class);
            for (ForeignKey fk : foreignKeys) {
                if (!existingFks.contains(fk.name().toLowerCase())) {
                    String sql = "ALTER TABLE `" + tableName + "` ADD " + buildForeignKeyDefinition(fk) + ";";
                    jdbcTemplate.execute(sql);
                    log.info("表 {} 新增外键: {}", tableName, fk.name());
                }
            }
        } catch (Exception e) {
            log.error("同步表 {} 外键失败: {}", tableName, e.getMessage());
        }
    }

    private String buildColumnDefinition(Field field) {
        String columnName = getColumnName(field);
        if (columnName == null) return null;

        StringBuilder sb = new StringBuilder();
        sb.append("`").append(columnName).append("` ");

        String dataType = getDataType(field);
        sb.append(dataType);

        // 主键处理
        TableId tableId = field.getAnnotation(TableId.class);
        if (tableId != null) {
            sb.append(" PRIMARY KEY");
            if (tableId.type() == IdType.AUTO) {
                sb.append(" AUTO_INCREMENT");
            }
        }

        // 默认值处理（修复引号问题）
        if (field.isAnnotationPresent(DefaultValue.class)) {
            String val = field.getAnnotation(DefaultValue.class).value();
            sb.append(" DEFAULT ").append(formatDefaultValue(field, val));
        }

        // 注释
        if (field.isAnnotationPresent(ColumnComment.class)) {
            sb.append(" COMMENT '").append(field.getAnnotation(ColumnComment.class).value()).append("'");
        }

        return sb.toString();
    }

    /**
     * 格式化默认值：如果是字符串且不是SQL函数，自动加单引号
     */
    private String formatDefaultValue(Field field, String value) {
        if (value == null || value.equalsIgnoreCase("NULL")) return "NULL";

        // 如果是常见的SQL函数，不加引号
        if (value.toUpperCase().contains("(") || value.equalsIgnoreCase("CURRENT_TIMESTAMP")) {
            return value;
        }

        Class<?> type = field.getType();
        if (type == String.class || type == LocalDateTime.class) {
            if (!value.startsWith("'")) {
                return "'" + value + "'";
            }
        }
        return value;
    }

    private String getColumnName(Field field) {
        TableId tableId = field.getAnnotation(TableId.class);
        if (tableId != null) {
            return tableId.value().isEmpty() ? field.getName() : tableId.value();
        }
        TableField tableField = field.getAnnotation(TableField.class);
        if (tableField != null && tableField.exist()) {
            return tableField.value().isEmpty() ? field.getName() : tableField.value();
        }
        // 如果没有任何注解，Mybatis-Plus 默认也是映射的，但为安全起见，此处逻辑要求显式注解或根据需求调整
        return null;
    }

    private boolean tableExists(String tableName) {
        String sql = "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName);
        return count != null && count > 0;
    }

    private Map<String, String> getExistingColumnsInfo(String tableName) {
        String sql = "SELECT COLUMN_NAME, COLUMN_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?";
        Map<String, String> map = new HashMap<>();
        jdbcTemplate.query(sql, (rs) -> {
            map.put(rs.getString("COLUMN_NAME").toLowerCase(), rs.getString("COLUMN_TYPE"));
        }, tableName);
        return map;
    }

    private Set<String> getExistingIndexNames(String tableName) {
        String sql = "SELECT DISTINCT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME != 'PRIMARY'";
        List<String> list = jdbcTemplate.queryForList(sql, String.class, tableName);
        return list.stream().map(String::toLowerCase).collect(Collectors.toSet());
    }

    private Set<String> getExistingForeignKeyNames(String tableName) {
        String sql = "SELECT CONSTRAINT_NAME FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = ? AND CONSTRAINT_TYPE = 'FOREIGN KEY'";
        List<String> list = jdbcTemplate.queryForList(sql, String.class, tableName);
        return list.stream().map(String::toLowerCase).collect(Collectors.toSet());
    }

    private String getTableCommentSql(Class<?> entityClass) {
        if (entityClass.isAnnotationPresent(TableComment.class)) {
            return " COMMENT='" + entityClass.getAnnotation(TableComment.class).value() + "'";
        }
        return "";
    }

    private List<String> getIndexDefinitionsSQL(Class<?> entityClass) {
        Index[] indexes = entityClass.getAnnotationsByType(Index.class);
        return Arrays.stream(indexes).map(this::buildIndexDefinition).collect(Collectors.toList());
    }

    private String buildIndexDefinition(Index index) {
        StringBuilder sb = new StringBuilder();
        switch (index.type()) {
            case UNIQUE -> sb.append("UNIQUE KEY ");
            case FULLTEXT -> sb.append("FULLTEXT KEY ");
            default -> sb.append("KEY ");
        }
        sb.append("`").append(index.name()).append("` ");
        sb.append("(`").append(String.join("`,`", index.columns())).append("`) ");
        sb.append("USING BTREE");
        if (!index.comment().isEmpty()) {
            sb.append(" COMMENT '").append(index.comment()).append("'");
        }
        return sb.toString();
    }

    private String getDataType(Field field) {
        if (field.isAnnotationPresent(ColumnType.class)) {
            return field.getAnnotation(ColumnType.class).value();
        }
        Class<?> fieldType = field.getType();
        if (fieldType.equals(String.class)) return "varchar(255)";
        if (fieldType.equals(LocalDateTime.class)) return "datetime";
        if (fieldType.equals(Long.class) || fieldType.equals(long.class)) return "bigint";
        if (fieldType.equals(Integer.class) || fieldType.equals(int.class)) return "int";
        if (fieldType.equals(Double.class) || fieldType.equals(double.class)) return "double";
        if (fieldType.equals(Boolean.class) || fieldType.equals(boolean.class)) return "tinyint(1)";
        if (fieldType.equals(BigDecimal.class)) return "decimal(19,2)";
        if (Collection.class.isAssignableFrom(fieldType) || Map.class.isAssignableFrom(fieldType)) return "json";
        return "varchar(255)";
    }

    private String buildForeignKeyDefinition(ForeignKey fk) {
        if (fk.columns().length != fk.referencedColumns().length) {
            throw new IllegalArgumentException("外键定义错误: columns 与 referencedColumns 长度不一致 [" + fk.name() + "]");
        }
        TableName refAnn = fk.referenceEntity().getAnnotation(TableName.class);
        if (refAnn == null) {
            throw new IllegalArgumentException("外键引用实体 " + fk.referenceEntity().getSimpleName() + " 缺失 @TableName");
        }

        String columnsSql = "`" + String.join("`,`", fk.columns()) + "`";
        String refColumnsSql = "`" + String.join("`,`", fk.referencedColumns()) + "`";
        String onDelete = "ON DELETE " + fk.onDelete().name().replace('_', ' ');
        String onUpdate = "ON UPDATE " + fk.onUpdate().name().replace('_', ' ');

        return String.format("CONSTRAINT `%s` FOREIGN KEY (%s) REFERENCES `%s` (%s) %s %s",
                fk.name(), columnsSql, refAnn.value(), refColumnsSql, onDelete, onUpdate);
    }

    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null && !clazz.equals(Object.class)) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    private boolean isIgnoreField(Field field) {
        return Modifier.isStatic(field.getModifiers())
                || Modifier.isTransient(field.getModifiers())
                || "serialVersionUID".equals(field.getName());
    }
}
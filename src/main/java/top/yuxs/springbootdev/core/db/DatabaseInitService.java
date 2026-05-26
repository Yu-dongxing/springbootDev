/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/04/11
 */

package top.yuxs.springbootdev.core.db;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.yuxs.springbootdev.core.db.annotation.ForeignKey;
import top.yuxs.springbootdev.core.db.annotation.Index;
import top.yuxs.springbootdev.core.db.config.AegisDbProperties;
import top.yuxs.springbootdev.core.db.metadata.ColumnMetadata;
import top.yuxs.springbootdev.core.db.metadata.TableMetadata;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 数据库初始化服务：负责数据库结构的自动同步与数据初始化
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseInitService {

    private final EntityScanner entityScanner;
    private final TableMetadataParser metadataParser;
    private final SqlGenerator sqlGenerator;
    private final SchemaExecutor schemaExecutor;
    private final AegisDbProperties properties;

    /**
     * 初始化入口
     */
    public void initDatabase() {
        if (!properties.isEnabled()) {
            log.info("数据库初始化已禁用");
            return;
        }

        log.info("开始数据库初始化，扫描包: {}", properties.getBasePackage());
        List<Class<?>> entityClasses = entityScanner.scanEntityClasses();
        List<TableMetadata> tables = entityClasses.stream()
                .map(metadataParser::parse)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 第一阶段：创建或更新表结构、索引（不含外键）
        for (TableMetadata table : tables) {
            syncTableStructure(table);
        }

        // 第二阶段：统一处理外键约束（解决循环依赖）
        for (TableMetadata table : tables) {
            syncForeignKeys(table);
        }

        // 第三阶段：初始化默认数据
        if (properties.isInitData()) {
            initDefaultData(tables);
        }

        log.info("数据库初始化完成");
    }

    /**
     * 第一阶段：同步表结构（列新增/修改，索引同步）
     */
    private void syncTableStructure(TableMetadata table) {
        String tableName = table.getTableName();
        try {
            if (!schemaExecutor.tableExists(tableName)) {
                log.info("表 {} 不存在，准备创建", tableName);
                schemaExecutor.execute(sqlGenerator.generateCreateTableSql(table));
            } else {
                updateTableColumnsAndIndexes(table);
            }
        } catch (Exception e) {
            log.error("同步表 {} 结构失败: {}", tableName, e.getMessage(), e);
            throw new RuntimeException("数据库建表或结构同步失败，表名: " + tableName, e);
        }
    }

    private void updateTableColumnsAndIndexes(TableMetadata table) {
        String tableName = table.getTableName();
        Map<String, ColumnMetadata> existingColumnsMap = schemaExecutor.getExistingColumnsInfo(tableName);
        Set<String> existingIndexes = schemaExecutor.getExistingIndexNames(tableName);

        for (ColumnMetadata column : table.getColumns()) {
            String columnName = column.getName();
            String targetTypeOnly = column.getType().toLowerCase().replaceAll("\\s", "");

            if (!existingColumnsMap.containsKey(columnName.toLowerCase())) {
                log.info("表 {} 新增列: {}", tableName, columnName);
                schemaExecutor.execute(sqlGenerator.generateAddColumnSql(tableName, column));
            } else {
                ColumnMetadata existingCol = existingColumnsMap.get(columnName.toLowerCase());
                String existingType = existingCol.getType().toLowerCase().replaceAll("\\s", "");
                String existingComment = existingCol.getComment() != null ? existingCol.getComment().trim() : "";
                String targetComment = column.getComment() != null ? column.getComment().trim() : "";
                
                boolean typeChanged = !targetTypeOnly.equals(existingType);
                boolean commentChanged = !targetComment.equals(existingComment);
                
                if (typeChanged || commentChanged) {
                    if (typeChanged) {
                        log.info("表 {} 变更列类型: {} -> {}", tableName, existingType, targetTypeOnly);
                    }
                    if (commentChanged) {
                        log.info("表 {} 变更列注释: '{}' -> '{}'", tableName, existingComment, targetComment);
                    }
                    schemaExecutor.execute(sqlGenerator.generateModifyColumnSql(tableName, column));
                }
            }
        }

        // 索引同步
        for (Index index : table.getIndexes()) {
            if (!existingIndexes.contains(index.name().toLowerCase())) {
                log.info("表 {} 新增索引: {}", tableName, index.name());
                schemaExecutor.execute(sqlGenerator.generateCreateIndexSql(tableName, index));
            }
        }
    }

    /**
     * 第二阶段：同步外键
     */
    private void syncForeignKeys(TableMetadata table) {
        String tableName = table.getTableName();
        try {
            Set<String> existingFks = schemaExecutor.getExistingForeignKeyNames(tableName);
            for (ForeignKey fk : table.getForeignKeys()) {
                if (!existingFks.contains(fk.name().toLowerCase())) {
                    log.info("表 {} 新增外键: {}", tableName, fk.name());
                    schemaExecutor.execute(sqlGenerator.generateAddForeignKeySql(tableName, fk));
                }
            }
        } catch (Exception e) {
            log.error("同步表 {} 外键失败: {}", tableName, e.getMessage(), e);
            throw new RuntimeException("同步表外键失败，表名: " + tableName, e);
        }
    }

    /**
     * 初始化默认数据
     */
    private void initDefaultData(List<TableMetadata> tables) {
        log.info("开始检查并初始化默认数据...");
        for (TableMetadata table : tables) {
            String tableName = table.getTableName();
            if (schemaExecutor.isTableEmpty(tableName)) {
                insertDefaultData(table);
            }
        }
    }

    private void insertDefaultData(TableMetadata table) {
        Map<String, Object> defaultData = table.getDefaultData();
        if (defaultData == null || defaultData.isEmpty()) return;

        String tableName = table.getTableName();
        String sql = sqlGenerator.generateInsertDefaultDataSql(tableName, defaultData);
        if (sql == null) return;

        try {
            log.info("表 {} 插入默认数据: {}", tableName, defaultData);
            schemaExecutor.executeWithParams(sql, defaultData.values().toArray());
        } catch (Exception e) {
            log.error("表 {} 插入默认数据失败: {}", tableName, e.getMessage(), e);
            throw new RuntimeException("插入表 " + tableName + " 默认数据失败", e);
        }
    }
}

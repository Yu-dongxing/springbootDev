/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/05/10
 */

package top.yuxs.springbootdev.core.db;

import com.baomidou.mybatisplus.annotation.TableName;
import org.springframework.stereotype.Component;
import top.yuxs.springbootdev.core.db.annotation.ForeignKey;
import top.yuxs.springbootdev.core.db.annotation.Index;
import top.yuxs.springbootdev.core.db.metadata.ColumnMetadata;
import top.yuxs.springbootdev.core.db.metadata.TableMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SQL 生成器：负责根据元数据生成 MySQL SQL 语句
 */
@Component
public class SqlGenerator {

    /**
     * 生成建表 SQL
     */
    public String generateCreateTableSql(TableMetadata table) {
        StringBuilder sql = new StringBuilder("CREATE TABLE `").append(table.getTableName()).append("` (");
        List<String> definitions = new ArrayList<>();

        for (ColumnMetadata column : table.getColumns()) {
            definitions.add(buildColumnDefinition(column));
        }

        for (Index index : table.getIndexes()) {
            definitions.add(buildIndexDefinition(index));
        }

        sql.append(String.join(", ", definitions)).append(")");
        if (table.getTableComment() != null && !table.getTableComment().isEmpty()) {
            sql.append(" COMMENT='").append(table.getTableComment()).append("'");
        }
        sql.append(";");
        return sql.toString();
    }

    /**
     * 生成新增列 SQL
     */
    public String generateAddColumnSql(String tableName, ColumnMetadata column) {
        return String.format("ALTER TABLE `%s` ADD COLUMN %s;", tableName, buildColumnDefinition(column));
    }

    /**
     * 生成修改列 SQL
     */
    public String generateModifyColumnSql(String tableName, ColumnMetadata column) {
        return String.format("ALTER TABLE `%s` MODIFY COLUMN %s;", tableName, buildColumnDefinition(column));
    }

    /**
     * 生成新增索引 SQL
     */
    public String generateCreateIndexSql(String tableName, Index index) {
        return "ALTER TABLE `" + tableName + "` ADD " + buildIndexDefinition(index) + ";";
    }

    /**
     * 生成新增外键 SQL
     */
    public String generateAddForeignKeySql(String tableName, ForeignKey fk) {
        TableName refAnn = fk.referenceEntity().getAnnotation(TableName.class);
        if (refAnn == null) {
            throw new IllegalArgumentException("外键引用实体 " + fk.referenceEntity().getSimpleName() + " 缺失 @TableName");
        }

        String columnsSql = "`" + String.join("`,`", fk.columns()) + "`";
        String refColumnsSql = "`" + String.join("`,`", fk.referencedColumns()) + "`";
        String onDelete = "ON DELETE " + fk.onDelete().name().replace('_', ' ');
        String onUpdate = "ON UPDATE " + fk.onUpdate().name().replace('_', ' ');

        return String.format("ALTER TABLE `%s` ADD CONSTRAINT `%s` FOREIGN KEY (%s) REFERENCES `%s` (%s) %s %s;",
                tableName, fk.name(), columnsSql, refAnn.value(), refColumnsSql, onDelete, onUpdate);
    }

    /**
     * 生成插入默认数据 SQL
     */
    public String generateInsertDefaultDataSql(String tableName, Map<String, Object> defaultData) {
        if (defaultData.isEmpty()) return null;

        StringBuilder sql = new StringBuilder("INSERT INTO `").append(tableName).append("` (");
        StringBuilder placeholders = new StringBuilder();

        List<String> columns = new ArrayList<>(defaultData.keySet());
        for (int i = 0; i < columns.size(); i++) {
            sql.append("`").append(columns.get(i)).append("`").append(i == columns.size() - 1 ? "" : ", ");
            placeholders.append("?").append(i == columns.size() - 1 ? "" : ", ");
        }

        sql.append(") VALUES (").append(placeholders).append(");");
        return sql.toString();
    }

    private String buildColumnDefinition(ColumnMetadata column) {
        StringBuilder sb = new StringBuilder();
        sb.append("`").append(column.getName()).append("` ");
        sb.append(column.getType());

        if (column.isPrimaryKey()) {
            sb.append(" PRIMARY KEY");
            if (column.isAutoIncrement()) {
                sb.append(" AUTO_INCREMENT");
            }
        }

        if (column.getDefaultValue() != null) {
            sb.append(" DEFAULT ").append(column.getDefaultValue());
        }

        if (column.getComment() != null && !column.getComment().isEmpty()) {
            sb.append(" COMMENT '").append(column.getComment()).append("'");
        }

        return sb.toString();
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
}

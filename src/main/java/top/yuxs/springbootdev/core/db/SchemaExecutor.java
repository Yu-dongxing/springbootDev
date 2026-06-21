/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/05/10
 */

package top.yuxs.springbootdev.core.db;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 结构执行器：负责执行 SQL 并提供数据库元数据查询
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaExecutor {

    private final JdbcTemplate jdbcTemplate;
    private Boolean isH2 = null;

    public boolean isH2Database() {
        if (isH2 == null) {
            try (java.sql.Connection conn = jdbcTemplate.getDataSource().getConnection()) {
                String dbProduct = conn.getMetaData().getDatabaseProductName();
                isH2 = "H2".equalsIgnoreCase(dbProduct);
            } catch (Exception e) {
                isH2 = false;
            }
        }
        return isH2;
    }

    public boolean tableExists(String tableName) {
        String sql = "SELECT COUNT(*) FROM information_schema.TABLES WHERE (LOWER(TABLE_SCHEMA) = LOWER(DATABASE()) OR (LOWER(TABLE_SCHEMA) = 'public' AND LOWER(TABLE_CATALOG) = LOWER(DATABASE()))) AND TABLE_NAME = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName);
        return count != null && count > 0;
    }

    public Map<String, top.yuxs.springbootdev.core.db.metadata.ColumnMetadata> getExistingColumnsInfo(String tableName) {
        String sql = "SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_COMMENT FROM information_schema.COLUMNS WHERE (LOWER(TABLE_SCHEMA) = LOWER(DATABASE()) OR (LOWER(TABLE_SCHEMA) = 'public' AND LOWER(TABLE_CATALOG) = LOWER(DATABASE()))) AND TABLE_NAME = ?";
        Map<String, top.yuxs.springbootdev.core.db.metadata.ColumnMetadata> map = new HashMap<>();
        jdbcTemplate.query(sql, (rs) -> {
            String colName = rs.getString("COLUMN_NAME");
            String colType = rs.getString("COLUMN_TYPE");
            String colComment = rs.getString("COLUMN_COMMENT");
            
            top.yuxs.springbootdev.core.db.metadata.ColumnMetadata metadata = top.yuxs.springbootdev.core.db.metadata.ColumnMetadata.builder()
                    .name(colName)
                    .type(colType)
                    .comment(colComment)
                    .build();
            map.put(colName.toLowerCase(), metadata);
        }, tableName);
        return map;
    }

    public Set<String> getExistingIndexNames(String tableName) {
        String sql = "SELECT DISTINCT INDEX_NAME FROM information_schema.STATISTICS WHERE (LOWER(TABLE_SCHEMA) = LOWER(DATABASE()) OR (LOWER(TABLE_SCHEMA) = 'public' AND LOWER(TABLE_CATALOG) = LOWER(DATABASE()))) AND TABLE_NAME = ? AND INDEX_NAME != 'PRIMARY'";
        List<String> list = jdbcTemplate.queryForList(sql, String.class, tableName);
        return list.stream().map(String::toLowerCase).collect(Collectors.toSet());
    }

    public Set<String> getExistingForeignKeyNames(String tableName) {
        String sql = "SELECT CONSTRAINT_NAME FROM information_schema.TABLE_CONSTRAINTS WHERE (LOWER(CONSTRAINT_SCHEMA) = LOWER(DATABASE()) OR (LOWER(CONSTRAINT_SCHEMA) = 'public' AND LOWER(CONSTRAINT_CATALOG) = LOWER(DATABASE()))) AND TABLE_NAME = ? AND CONSTRAINT_TYPE = 'FOREIGN KEY'";
        List<String> list = jdbcTemplate.queryForList(sql, String.class, tableName);
        return list.stream().map(String::toLowerCase).collect(Collectors.toSet());
    }

    public boolean isTableEmpty(String tableName) {
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `" + tableName + "`", Integer.class);
            return count != null && count == 0;
        } catch (Exception e) {
            log.warn("检查表 {} 是否为空时出错: {}", tableName, e.getMessage());
            return false;
        }
    }

    public void execute(String sql) {
        log.info("执行 SQL: {}", sql);
        jdbcTemplate.execute(sql);
    }

    public void executeWithParams(String sql, Object[] params) {
        log.info("执行 SQL (带参数): {}, 参数: {}", sql, params);
        jdbcTemplate.update(sql, params);
    }
}

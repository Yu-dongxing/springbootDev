/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/05/10
 */

package top.yuxs.springbootdev.core.db.metadata;

import lombok.Builder;
import lombok.Data;
import top.yuxs.springbootdev.core.db.annotation.ForeignKey;
import top.yuxs.springbootdev.core.db.annotation.Index;

import java.util.List;
import java.util.Map;

/**
 * 表元数据
 */
@Data
@Builder
public class TableMetadata {
    private String tableName;
    private String tableComment;
    private List<ColumnMetadata> columns;
    private List<Index> indexes;
    private List<ForeignKey> foreignKeys;
    private Map<String, Object> defaultData;
}

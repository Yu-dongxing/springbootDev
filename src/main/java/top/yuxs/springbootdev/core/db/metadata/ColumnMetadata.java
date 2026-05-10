/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/05/10
 */

package top.yuxs.springbootdev.core.db.metadata;

import lombok.Builder;
import lombok.Data;

/**
 * 列元数据
 */
@Data
@Builder
public class ColumnMetadata {
    private String name;
    private String type;
    private boolean isPrimaryKey;
    private boolean isAutoIncrement;
    private String defaultValue;
    private String comment;
}

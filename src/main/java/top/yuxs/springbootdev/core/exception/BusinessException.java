/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/04/11
 */

package top.yuxs.springbootdev.core.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private int code;
    private Object data;
    private String auditMessage;

    public BusinessException(int code, String msg) {
        super(msg);
        this.code = code;
    }
    public BusinessException(String msg) {
        super(msg);
        this.code = 0;
    }
    public BusinessException(String msg, String auditMessage) {
        super(msg);
        this.code = 0;
        this.auditMessage = auditMessage;
    }
    public BusinessException(int code, String msg, Object data) {
        super(msg);
        this.code = code;
        this.data = data;
    }
}

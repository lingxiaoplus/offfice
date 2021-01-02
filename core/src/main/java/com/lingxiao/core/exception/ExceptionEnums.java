package com.lingxiao.core.exception;

/**
 * @author renml
 * @date 2020/12/22 16:03
 */
public enum ExceptionEnums {
    /**
     * com.lingxiao.office callback error
     */
    KEY_IDENTIFIER_NOT_FOUND(0,"no document with the key identifier could be found"),
    SAVING_FILE_ERROR(3,"document saving error has occurred"),
    FORCE_SAVING_FILE_ERROR(7,"error has occurred while force saving the document"),
    ;
    private final int code;
    private final String msg;
    ExceptionEnums(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}

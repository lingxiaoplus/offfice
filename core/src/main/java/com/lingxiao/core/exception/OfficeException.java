package com.lingxiao.core.exception;

/**
 * @author Admin
 */
public class OfficeException extends RuntimeException{
    public OfficeException(String message) {
        super(message);
    }
    public OfficeException(ExceptionEnums exceptionEnums) {
        super(exceptionEnums.getMsg());
    }
}

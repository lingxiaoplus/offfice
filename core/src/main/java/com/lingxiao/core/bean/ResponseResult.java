package com.lingxiao.core.bean;

import lombok.Data;
import org.springframework.lang.Nullable;


/**
 * @author Admin
 */
@Data
public class ResponseResult<T> {
    private int code = 200;
    private String message = "ok";
    private T data;

    public ResponseResult(T data){
        this.data = data;
    }

    private ResponseResult() {
    }

    public ResponseResult(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public static <T> ResponseResult<T> ok (@Nullable T data){
        return new ResponseResult<>(data);
    }
    public static <T> ResponseResult<T> error (int code, String message){
        return new ResponseResult<>(code,message);
    }
}
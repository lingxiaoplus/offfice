package com.lingxiao.core.exception;

import com.lingxiao.core.bean.ResponseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.List;

/**
 * @author admin
 */
@ControllerAdvice
@Slf4j
public class CommonExceptionHandler {

    @ExceptionHandler(OfficeException.class)
    public ResponseEntity<ResponseResult<Object>> handleException(OfficeException e) {
        return ResponseEntity.status(HttpStatus.OK).body(ResponseResult.error(HttpStatus.INTERNAL_SERVER_ERROR.value(),e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseResult<Object>> handleValidException(MethodArgumentNotValidException e) {
        List<ObjectError> allErrors = e.getBindingResult().getAllErrors();
        StringBuilder stringBuilder = new StringBuilder();
        allErrors.forEach(error -> stringBuilder.append(error.getDefaultMessage()).append(", "));
        return ResponseEntity.status(HttpStatus.OK).
                body(ResponseResult.error(HttpStatus.INTERNAL_SERVER_ERROR.value(),stringBuilder.toString()));
    }

    /**
     * 处理所有不可知异常
     *
     * @param throwable
     * @return json
     */
    @ExceptionHandler(Throwable.class)
    @ResponseBody
    public ResponseEntity<ResponseResult<Object>> handlerException(Throwable throwable) {
        throwable.printStackTrace();
        return ResponseEntity.status(HttpStatus.OK).
                body(ResponseResult.error(HttpStatus.INTERNAL_SERVER_ERROR.value(),throwable.getMessage()));
    }
}

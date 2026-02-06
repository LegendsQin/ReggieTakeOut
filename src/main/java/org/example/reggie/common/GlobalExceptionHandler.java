package org.example.reggie.common;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLIntegrityConstraintViolationException;


/**
 * 全局异常处理类,用于处理应用程序中抛出的异常
 */
@ControllerAdvice(annotations = {RestController.class, Controller.class})
@Slf4j
@ResponseBody
public class GlobalExceptionHandler {

    /**
     * 异常处理方法，处理SQLIntegrityConstraintViolationException异常
     * @param e
     * @return
     */
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public R<String> exceptionHandler(SQLIntegrityConstraintViolationException e) {
        log.error("异常信息：{}", e.getMessage());
        String message = e.getMessage();
        if(message.contains("Duplicate entry")){
            String[] temp = message.split(" ");
            String username = temp[2];
            return R.error("账号:"+username+"已存在");
        }
        return R.error("系统异常");
    }

    /**
     * 异常处理方法，处理CustomException异常
     * @param e
     * @return
     */
    @ExceptionHandler(CustomException.class)
    public R<String> exceptionHandler(CustomException e) {
        log.error("异常信息：{}", e.getMessage());
        return R.error(e.getMessage());
    }
}

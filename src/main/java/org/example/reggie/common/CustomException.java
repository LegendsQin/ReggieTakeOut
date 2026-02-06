package org.example.reggie.common;



/**
 * 自定义异常类,用于在应用程序中抛出业务逻辑异常
 * 在GlobalExceptionHandler中被调用为一种新的异常处理方法
 *
 * 直接通过throw new CustomException("异常信息")来调用该类即可抛出异常
 */

public class CustomException extends RuntimeException
{

    public CustomException(String message)
    {
        super(message);
    }
}

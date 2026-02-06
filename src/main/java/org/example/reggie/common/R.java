package org.example.reggie.common;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;


/**
 * 通用返回结果类,服务端响应的数据最终都会封装成此对象
 * @param <T>
 */
//你这个 R<T> 通用返回类如果不加 @Data 注解，核心问题是缺失自动生成的 getter/setter 等方法，会导致前端 / 其他代码无法正常获取 / 设置类中的字段（code/msg/data/map），最终引发编译错误或运行时异常
@Data   //你问的 @Data 是 Lombok 框架提供的核心注解，核心作用是自动帮你生成 Java 类中繁琐的模板代码（比如 getter/setter、toString、equals 等），让实体类代码极度简洁
public class R<T> {

    private Integer code; //编码：1成功，0和其它数字为失败

    private String msg; //错误信息

    private T data; //数据

    private Map map = new HashMap(); //动态数据

    public static <T> R<T> success(T object) {
        R<T> r = new R<T>();
        r.data = object;
        r.code = 1;
        return r;
    }


    public static <T> R<T> error(String msg) {
        R r = new R();
        r.msg = msg;
        r.code = 0;
        return r;
    }

    public R<T> add(String key, Object value) {
        this.map.put(key, value);
        return this;
    }

}

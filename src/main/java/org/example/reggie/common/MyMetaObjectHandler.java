package org.example.reggie.common;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;


/**
 * 自定义元数据对象处理器,将多个实体类都包含的公共属性统一进行填充赋值,包括创建时间、更新时间、创建人、更新人
 */
@Component
@Slf4j
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {

        //metaObject.setValue("createTime", LocalDateTime.now());   // 不管这个字段原来有没有值，都会被覆盖（比如你手动设置了 createTime，也会被替换成当前时间）；
        // 为实体类的 createTime 和 updateTime 字段设置当前时间
        this.setFieldValByName("createTime", LocalDateTime.now(), metaObject);  // 默认只填充 null 值（如果你手动设置了 createTime，不会被覆盖，保留你的自定义值）；
        this.setFieldValByName("updateTime", LocalDateTime.now(), metaObject);
        this.setFieldValByName("createUser", BaseContext.getCurrentUserId(), metaObject);
        this.setFieldValByName("updateUser", BaseContext.getCurrentUserId(), metaObject);

        log.info(metaObject.toString());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.setFieldValByName("updateTime", LocalDateTime.now(), metaObject);  // 默认只填充 null 值（如果你手动设置了 updateTime，不会被覆盖，保留你的自定义值）；
        this.setFieldValByName("updateUser", BaseContext.getCurrentUserId(), metaObject);
        log.info(metaObject.toString());
    }
}

package org.example.reggie.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置mybatisplus的分页插件
 */
@Configuration
public class MybatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        //PaginationInnerInterceptor 是 MyBatis-Plus 的可选插件，而非 “默认生效的功能”：
        //它本质是一个「待注册的拦截器组件」，必须通过 MybatisPlusInterceptor 这个 “插件容器” 手动注册到 Spring 容器中，才能被 MyBatis 识别并生效；
        //不配置的话，MP 完全不知道你要启用分页功能，page() 方法只会执行 “全表查询”，分页逻辑（拼接 LIMIT、计算总条数）全部失效。
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }
}

package org.example.reggie.common;


import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.teaopenapi.models.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


/**
 * 阿里云短信服务客户端工具类
 */
@Component
public class AliyunDyPnsClientUtil {
    /**
     * 阿里云短信服务AccessKeyId
     */
    @Value("${aliyun.access-key}")
    private String accessKeyId;

    /**
     * 阿里云短信服务AccessKeySecret
     */
    @Value("${aliyun.access-secret}")
    private String accessKeySecret;

    // 初始化客户端
    // 调用带 throws Exception 的方法：调用者必须使用 try-catch 块捕获异常，或者继续向上抛出异常
    public Client createClient() throws Exception {
        Config config = new Config()
                .setAccessKeyId(accessKeyId)
                .setAccessKeySecret(accessKeySecret);
        config.setEndpoint("dypnsapi.aliyuncs.com");
        return new Client(config);
    }
}

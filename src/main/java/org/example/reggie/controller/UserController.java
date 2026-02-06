package org.example.reggie.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.aliyun.tea.TeaException;
import com.aliyun.teautil.models.RuntimeOptions;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.example.reggie.common.AliyunDyPnsClientUtil;
import org.example.reggie.common.R;
import org.example.reggie.common.ValidateCodeUtils;
import org.example.reggie.entity.User;
import org.example.reggie.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;

/**
 * 用户信息
 */
@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private AliyunDyPnsClientUtil aliyunDyPnsClientUtil;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 获取验证码
     * @param user
     * @return
     */
    @PostMapping("/sendMsg")
    public R<String> getValidCode(@RequestBody User user) {
        // 1. 从请求体中获取手机号
        String phone = user.getPhone();
        if (StringUtils.isBlank(phone)) {
            return R.error("手机号不能为空");
        }

        Integer varifyCode = ValidateCodeUtils.generateValidateCode(6);

        // 验证码的有效期, 单位:分钟
        int expireMin = 30;

        try{
            // 步骤 1：创建阿里云短信服务客户端（Client）,创建一个能和阿里云短信服务器通信的「客户端对象」
            Client client = aliyunDyPnsClientUtil.createClient();
            // 步骤 2：构建短信发送请求参数（Request）,把发送短信需要的所有参数封装成一个「请求对象」
            SendSmsVerifyCodeRequest request = new SendSmsVerifyCodeRequest()
                    .setPhoneNumber(phone)
                    .setSignName("速通互联验证码")
                    .setTemplateCode("100001")
                    .setTemplateParam("{\"code\":\"" + varifyCode.toString() + "\",\"min\":\"" + expireMin + "\"}");

            // 步骤 3：创建运行时配置对象(RuntimeOptions), 设置请求的运行时参数（比如超时时间、重试策略等），这里用默认配置即可
            RuntimeOptions runtimeOptions = new RuntimeOptions();
            // 步骤 4：发送请求并获取响应
            SendSmsVerifyCodeResponse response = client.sendSmsVerifyCodeWithOptions(request, runtimeOptions);

            if("OK".equals(response.getBody().getCode())){
                // 验证码存入Redis，有效期5分钟
                redisTemplate.opsForValue().set("sms_code:" + phone, varifyCode.toString(), expireMin, TimeUnit.MINUTES);
                return R.success("验证码发送成功");
            }
            else {
                return R.error("验证码发送失败1");
            }
        }
        catch (TeaException e){
            log.error("验证码发送失败，异常信息：{}", e.getMessage());
            return R.error("验证码发送失败2");
        }
        catch (Exception e){
            log.error("验证码发送失败，异常信息：{}", e.getMessage());
            return R.error("验证码发送失败3");
        }

    }


    @PostMapping("/login")
    public R<String> login(HttpServletRequest request, @RequestBody JSONObject jsonObject) {

        // 1. 从请求体中获取手机号和验证码
        String phone = jsonObject.getString("phone");
        String verifyCode = jsonObject.getString("code");
        // 校验手机号和验证码是否为空
        if (StringUtils.isBlank(phone) || StringUtils.isBlank(verifyCode)) {
            return R.error("手机号或验证码不能为空");
        }


        // 2 从redis中根据该手机号获取正确的验证码
        String trueVerifyCode = redisTemplate.opsForValue().get("sms_code:" + phone);
        if (StringUtils.isBlank(trueVerifyCode)) {
            return R.error("验证码已过期");
        }

        if(!verifyCode.equals(trueVerifyCode)) {
            return R.error("输入的验证码有误");
        }

        // 3 验证通过,注册该手机号进入数据库表
        return userService.saveWithCheck(request, phone);


    }

    @PostMapping("/loginout")
    public R<String> loginOut(HttpServletRequest request) {
        request.getSession().removeAttribute("user");
        return R.success("退出成功");
    }
}

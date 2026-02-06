package org.example.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.reggie.common.R;
import org.example.reggie.entity.User;

import javax.servlet.http.HttpServletRequest;

public interface UserService extends IService<User> {

    /**
     * 检查手机号是否已注册,若未注册,则注册该手机号
     * @param phone
     * @return
     */
    public R<String> saveWithCheck(HttpServletRequest request, String phone);

}

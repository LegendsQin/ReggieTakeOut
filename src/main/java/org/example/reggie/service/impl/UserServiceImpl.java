package org.example.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.example.reggie.common.R;
import org.example.reggie.entity.User;
import org.example.reggie.mapper.UserMapper;
import org.example.reggie.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;


@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private UserService userService;

    /**
     * 检查手机号是否已注册,若未注册,则注册该手机号
     *
     * @param phone
     * @return
     */
    @Override
    public R<String> saveWithCheck(HttpServletRequest request, String phone) {

        try {
            // 1. 检查手机号是否已注册
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getPhone, phone);
            User user = userService.getOne(queryWrapper);
            if(user!=null && user.getStatus() == 0) {
                log.info("该手机号已被禁用,请联系管理员");
                return R.error("该手机号已被禁用,请联系管理员");
            }
            if(user!=null && user.getStatus() == 1) {
                request.getSession().setAttribute("user",user.getId());
                log.info("手机号已注册,成功登录");
                return R.success("手机号已注册,成功登录");
            }

            // 2. 若未注册,则注册该手机号
            User newUser = new User();
            newUser.setPhone(phone);
            newUser.setStatus(1);
            userService.save(newUser);
            request.getSession().setAttribute("user",newUser.getId());
            log.info("手机号未注册,成功注册");
            return R.success("注册成功");
        }
        catch (Exception e) {
            log.error("注册失败，异常信息：{}", e.getMessage());
            return R.error("注册失败，异常信息：" + e.getMessage());
        }
    }
}

package org.example.reggie.filter;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.example.reggie.common.BaseContext;
import org.example.reggie.common.R;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 登录检查过滤器,用于检查用户是否已登录,如果未登录,则重定向到登录页面
 */
@Slf4j
@WebFilter(filterName = "loginCheckFilter", urlPatterns = "/*")
public class LoginCheckFilter implements Filter {

    // 定义不需要登录的URL白名单
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/employee/login",
            "/employee/logout",
            "/backend/**",
            "/front/**",
            "/common/**",
            "/user/login",
            "/user/sendMsg"
    );

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        // 1. 获取当前请求的URI
        String requestURI = request.getRequestURI();
        log.info("拦截到请求：{}", requestURI);

        // 2. 判断本次请求是否需要处理（检查白名单）
        if (checkWhiteList(requestURI)) {
            log.info("本次请求{}不需要处理", requestURI);
            filterChain.doFilter(request, response);
            return;
        }


        /**
         * Threadlocal 答疑：
         * 在 Web 应用（如基于 Spring Boot + Tomcat）中：
         * 每个 HTTP 请求由一个独立的线程处理,
         * 所以下边的 BaseContext.setCurrentUserId(empId);和BaseContext.setCurrentUserId(userId);永远不会在同一个线程内被执行
         * 线程由 Tomcat 的线程池分配
         * 不同请求可能由不同线程处理
         * 同一个请求的所有处理（过滤器、控制器、业务逻辑）都在同一个线程中完成
         */


        // 3. 判断backend用户登录状态，如果已登录，则直接放行
        Long empId = (Long) request.getSession().getAttribute("employee");
        if (empId != null) {
            log.info("用户已登录，用户id为：{}", empId);
            // 4.1 调用BaseContext设置当前登录用户的ID
            BaseContext.setCurrentUserId(empId);
            try {
                filterChain.doFilter(request, response);
            }
            finally {
                // 4.2 移除当前登录用户的ID
                BaseContext.removeCurrentUserId();
            }
            return;
        }

        // 3. 判断front顾客登录状态，如果已登录，则直接放行
        Long userId = (Long) request.getSession().getAttribute("user");
        if (userId != null) {
            log.info("顾客已登录，顾客id为：{}", userId);
            // 4.1 调用BaseContext设置当前登录顾客的ID
            BaseContext.setCurrentUserId(userId);
            try {
                filterChain.doFilter(request, response);
            }
            finally {
                // 4.2 移除当前登录顾客的ID
                BaseContext.removeCurrentUserId();
            }
            return;
        }

        // 4. 如果未登录，则重定向到登录页面
        log.info("用户未登录");
        response.getWriter().write(JSON.toJSONString(R.error("NOTLOGIN")));

    }

    /**
     * 检查请求路径是否在白名单中
     */
    private boolean checkWhiteList(String requestURI) {
        for (String url : WHITE_LIST) {
            // 处理通配符匹配
            if (url.contains("**")) {
                String pattern = url.replace("**", ".*");
                if (requestURI.matches(pattern)) {
                    return true;
                }
            } else if (requestURI.equals(url)) {
                return true;
            }
        }
        return false;
    }
}
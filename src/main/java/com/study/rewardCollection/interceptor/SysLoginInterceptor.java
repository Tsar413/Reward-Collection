package com.study.rewardCollection.interceptor;

import com.study.rewardCollection.entity.SysUser;
import com.study.rewardCollection.service.SysLoginService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SysLoginInterceptor implements HandlerInterceptor {

    @Resource
    private SysLoginService service;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 放行跨域预检请求。
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        String token = authorization.substring(7).trim();
        // 检查Token、到期时间、撤销状态、用户和组织状态。
        // 校验失败会抛出异常，由全局异常处理器返回401。
        SysUser user = service.checkToken(token);
        request.setAttribute("loginUser", user);
        request.setAttribute("loginToken", token);
        return true;
    }
}
package com.ymsz.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
public class MyInterceptor implements HandlerInterceptor {

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        log.warn("这是MyInterceptor preHandle 方法 在业务处理器处理请求执行之前调用");
        return true;
    }

    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable ModelAndView modelAndView) throws Exception {
        log.warn("这是MyInterceptor postHandle 方法 在业务处理器处理请求执行后调用");
    }

    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        log.warn("这是MyInterceptor afterCompletion 方法 在DispatcherServlet完全处理完请求后被调用，可用于清理资源等");
    }


}

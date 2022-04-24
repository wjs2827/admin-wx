package com.ymsz.interceptor;

import com.ymsz.config.RedisCacheManager;
import com.ymsz.utils.Constants;
import com.ymsz.utils.TokenUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
public class MyInterceptor implements HandlerInterceptor {

    @Autowired
    private TokenUtils tokenUtils;

    @Qualifier("myRedisTemplate")
    @Autowired
    protected RedisTemplate redis;

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        log.warn("这是MyInterceptor preHandle 方法 在业务处理器处理请求执行之前调用");
        //设置跨域访问CORS
        this.setAllowCors(response);
        //判断是否授权失效
        String token = request.getHeader(Constants.header);
        if (tokenUtils.isTokenExpired(RedisCacheManager.getTokenCacheKey(token)) || !tokenUtils.verifyToken(token)) {
            response.setStatus(401);
            return false;
        }
        //根据 token 取出 userId
        Object userId = redis.opsForValue().get(RedisCacheManager.getTokenCacheKey(token));
        if (userId == null) {
            response.setStatus(401);
            return false;
        }
        request.setAttribute(Constants.USER_ID, userId);
        return true;
    }

    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable ModelAndView modelAndView) throws Exception {
        log.warn("这是MyInterceptor postHandle 方法 在业务处理器处理请求执行后调用");
        return;
    }

    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        log.warn("这是MyInterceptor afterCompletion 方法 在DispatcherServlet完全处理完请求后被调用，可用于清理资源等");
        return;
    }

    /**
     * 设置跨域参数
     * @param response
     */
    private void setAllowCors(HttpServletResponse response) {
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Methods", "POST,OPTIONS,PUT,HEAD");
        response.addHeader("Access-Control-Max-Age", "3600000");
        response.addHeader("Access-Control-Allow-Credentials", "true");
        response.addHeader("Access-Control-Allow-Headers", "Authentication,Origin, X-Requested-With, Content-Type, Accept,token");
        //让请求，不被缓存，
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
    }

}

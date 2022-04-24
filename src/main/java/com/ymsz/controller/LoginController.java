package com.ymsz.controller;

import com.alibaba.fastjson.JSONObject;
import com.ymsz.config.RedisCacheManager;
import com.ymsz.controller.base.BaseController;
import com.ymsz.exception.SignException;
import com.ymsz.service.LoginService;
import com.ymsz.utils.Constants;
import com.ymsz.utils.TokenBuilder;
import com.ymsz.utils.TokenUtils;
import com.ymsz.utils.VerifyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * @author jinshan.wang
 * @description
 * @date 2022/04/23
 * @email jinshan.wang.it@foxmail.com
 */
@RestController
public class LoginController extends BaseController {
    private LoginService service;
    private TokenUtils tokenUtils;

    @Autowired
    public LoginController(LoginService service, TokenUtils tokenUtils) {
        this.service = service;
        this.tokenUtils = tokenUtils;
    }

    @PostMapping(value = "/login")
    public String login(HttpServletRequest request, HttpServletResponse response) throws NoSuchAlgorithmException, SignException {
        String jsCode = request.getHeader(Constants.header);
        if (VerifyUtils.isNullOrEmpty(jsCode)) {
            response.setStatus(PARAM_ERROR_STATUS);
            return null;
        }
        Map<String, String> map = service.getOpenId(jsCode);
        System.out.println(map);
        String openid = map.get("openid");
        String sessionKey = map.get("session_key");
        String token = new TokenBuilder(tokenUtils)
                .setIssuedAt(System.currentTimeMillis())
                .setIssuer("happysnaker")
                .setSubject("auth_token")
                .setKV("random", Math.random() * 20211212)
                .build();
        map.put("token", token);
        tokenUtils.storeToken(token, openid, redis);
        String Key = RedisCacheManager.getTokenCacheKey(token);
        return JSONObject.toJSONString(map);
    }

}

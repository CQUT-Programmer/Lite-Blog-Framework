package com.lite.auth.interceptor;

import com.lite.auth.utils.LiteBlogContextUtils;
import com.lite.common.i18n.LocalMessages;
import com.lite.common.utils.JwtUtil;
import com.lite.auth.utils.Authenticator;
import com.lite.auth.exception.AuthException;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 身份校验拦截器，用于处理用户身份的校验，校验完成后会将用户的身份信息存入ThreadLocal,请求完成后将会移除
 */
@Order(2)
@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    Authenticator authenticator;

    @Autowired
    LiteBlogContextUtils contextUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {

        try {
            //获取请求头中的token
            String accessToken = request.getHeader(JwtUtil.JWT_ACCESS_KEY);

            //断言
            Assert.notNull(accessToken, LocalMessages.get("error.jwt.access.notNull"));

            if (!authenticator.authenticateAccessToken(accessToken))
                throw new AuthException(HttpStatus.FORBIDDEN.value(), LocalMessages.get("error.jwt.access.invalid"));


        } catch (ExpiredJwtException e) {
            //token过期
            response.sendError(HttpStatus.UNAUTHORIZED.value(), LocalMessages.get("error.jwt.access.expired"));
            return false;
        } catch (Exception e) {
            response.sendError(HttpStatus.FORBIDDEN.value(), LocalMessages.get("error.jwt.access.invalid"));
            return false;
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        //请求完成后移除，防止线程无法空闲而导致线程池溢出，最后内存泄露
        contextUtils.clearLocalUserInfo();
    }
}

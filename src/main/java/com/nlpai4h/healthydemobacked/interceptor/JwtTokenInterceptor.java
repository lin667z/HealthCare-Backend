package com.nlpai4h.healthydemobacked.interceptor;

import com.nlpai4h.healthydemobacked.common.constant.JwtClaimsConstant;
import com.nlpai4h.healthydemobacked.common.context.BaseContext;
import com.nlpai4h.healthydemobacked.common.exception.AuthException;
import com.nlpai4h.healthydemobacked.common.properties.JwtProperties;
import com.nlpai4h.healthydemobacked.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT token interceptor.
 */
@Component
@Slf4j
public class JwtTokenInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties jwtProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        BaseContext.removeCurrentUser();

        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        String token = request.getHeader(jwtProperties.getMxbTokenName());
        if (token == null || token.isBlank()) {
            token = request.getParameter(jwtProperties.getMxbTokenName());
        }

        try {
            if (token == null || token.isBlank()) {
                throw new AuthException("未登录或Token缺失");
            }
            Claims claims = JwtUtil.parseJWT(jwtProperties.getMxbSecretKey(), token);
            Long userId = Long.valueOf(claims.get(JwtClaimsConstant.USER_ID).toString());
            String username = claims.get(JwtClaimsConstant.USERNAME, String.class);
            String role = claims.get(JwtClaimsConstant.ROLE, String.class);

            BaseContext.setCurrentUser(userId, username, role);
            return true;
        } catch (ExpiredJwtException ex) {
            log.warn("JWT validation failed: type={}", ex.getClass().getSimpleName());
            throw new AuthException("Token已过期", ex);
        } catch (MalformedJwtException ex) {
            log.warn("JWT validation failed: type={}", ex.getClass().getSimpleName());
            throw new AuthException("Token格式错误", ex);
        } catch (UnsupportedJwtException ex) {
            log.warn("JWT validation failed: type={}", ex.getClass().getSimpleName());
            throw new AuthException("Token类型不支持", ex);
        } catch (SignatureException | SecurityException ex) {
            log.warn("JWT validation failed: type={}", ex.getClass().getSimpleName());
            throw new AuthException("Token签名错误", ex);
        } catch (AuthException ex) {
            log.warn("JWT validation failed: type={}", ex.getClass().getSimpleName());
            throw ex;
        } catch (Exception ex) {
            log.warn("JWT validation failed: type={}", ex.getClass().getSimpleName());
            throw new AuthException("Token校验失败", ex);
        }
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            @Nullable Exception ex
    ) {
        BaseContext.removeCurrentUser();
    }
}

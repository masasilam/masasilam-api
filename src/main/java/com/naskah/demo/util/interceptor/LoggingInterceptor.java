package com.naskah.demo.util.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class LoggingInterceptor implements HandlerInterceptor {

    private final LoggingHolder loggingHolder;
    private final JwtUtil jwtUtil;

    @Value("${app.version}")
    private String appVersion;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader("Authorization");
        Map<String, String> tokenValue = jwtUtil.getValueFromToken(token, new String[]{"name", "username", "role"});
        String name = tokenValue.getOrDefault("name", "");
        String username = tokenValue.getOrDefault("username", "");

        loggingHolder.setUser(String.format("User: name=%s, username=%s", name, username));
        loggingHolder.setIpAddress(getClientIpAddress(request));
        loggingHolder.setPath(request.getRequestURI() == null ? "" : request.getRequestURI());
        loggingHolder.setData(request.getQueryString() == null ? "" : request.getRequestURI());
        loggingHolder.setPackageName(getClass().getPackage().getName());
        loggingHolder.setVersion(appVersion != null ? appVersion : "Version Not Found!");
        loggingHolder.setDate(LocalDateTime.now());

        return true;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String[] ipHeaders = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR",
                "X-Real-IP"
        };

        for (String header : ipHeaders) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}

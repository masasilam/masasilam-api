package com.naskah.demo.util.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.springframework.web.servlet.HandlerInterceptor;
import java.util.Map;

@RequiredArgsConstructor
public class HeaderInterceptor implements HandlerInterceptor {

    private final HeaderHolder headerHolder;
    private final JwtUtil jwtUtil;
    private final UserAgentAnalyzer userAgentAnalyzer;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader("Authorization");
        Map<String, String> tokenValue = jwtUtil.getValueFromToken(token, new String[]{"name", "username", "role"});

        headerHolder.setName(tokenValue.getOrDefault("name", ""));
        headerHolder.setUsername(tokenValue.getOrDefault("username", ""));
        headerHolder.setRoles(tokenValue.getOrDefault("role", "").split(","));
        headerHolder.setIpAddress(getClientIpAddress(request));

        parseUserAgentWithYauaa(request);

        return true;
    }

    private void parseUserAgentWithYauaa(HttpServletRequest request) {
        String userAgentString = request.getHeader("User-Agent");

        if (userAgentString != null) {
            UserAgent agent = userAgentAnalyzer.parse(userAgentString);

            headerHolder.setDeviceType(agent.getValue(UserAgent.DEVICE_CLASS));
            headerHolder.setDeviceName(agent.getValue(UserAgent.DEVICE_NAME));
            headerHolder.setDeviceBrand(agent.getValue(UserAgent.DEVICE_BRAND));
            headerHolder.setBrowser(agent.getValue(UserAgent.AGENT_NAME_VERSION));
            headerHolder.setOs(agent.getValue(UserAgent.OPERATING_SYSTEM_NAME_VERSION));
            headerHolder.setLayoutEngine(agent.getValue(UserAgent.LAYOUT_ENGINE_NAME));
            headerHolder.setDeviceCpu(agent.getValue(UserAgent.DEVICE_CPU));
        } else {
            setUnknownValues();
        }
    }

    private void setUnknownValues() {
        headerHolder.setDeviceType("UNKNOWN");
        headerHolder.setDeviceName("UNKNOWN");
        headerHolder.setDeviceBrand("UNKNOWN");
        headerHolder.setBrowser("UNKNOWN");
        headerHolder.setOs("UNKNOWN");
        headerHolder.setLayoutEngine("UNKNOWN");
        headerHolder.setDeviceCpu("UNKNOWN");
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
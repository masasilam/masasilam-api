package com.naskah.demo.util.interceptor;

import lombok.RequiredArgsConstructor;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class HeaderInterceptorConfig implements WebMvcConfigurer {

    private final UserAgentAnalyzer userAgentAnalyzer;
    private final JwtUtil jwtUtil;

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(headerInterceptor());
    }

    @Bean
    public HeaderInterceptor headerInterceptor() {
        return new HeaderInterceptor(headerHolder(), jwtUtil, userAgentAnalyzer);
    }

    @Bean
    @Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
    public HeaderHolder headerHolder() {
        return new HeaderHolder();
    }
}

package com.naskah.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "vps")
public class VpsStorageProperties {
    private String host;
    private int port = 22;
    private String username;
    private String password;
    private String basePath;
    private String baseUrl;
}
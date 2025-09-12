package com.naskah.demo.util.interceptor;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class LoggingHolder {
    private String packageName;
    private LocalDateTime date;
    private String path;
    private String ipAddress;
    private String user;
    private String data;
    private String version;
}

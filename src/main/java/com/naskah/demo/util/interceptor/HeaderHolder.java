package com.naskah.demo.util.interceptor;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HeaderHolder {
    private String name;
    private String username;
    private String[] roles;
    private String ipAddress;
    private String deviceType;
    private String deviceName;
    private String deviceBrand;
    private String browser;
    private String os;
    private String layoutEngine;
    private String deviceCpu;
}

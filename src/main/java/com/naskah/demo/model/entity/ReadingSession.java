package com.naskah.demo.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReadingSession {
    private Long id;
    private Long userId;
    private Long bookId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String deviceClass;
    private String deviceName;
    private String deviceBrand;
    private String agentNameVersion;
    private String operatingSystem;
    private String layoutEngine;
    private String deviceCpu;
    private String ipAddress;
    private LocalDateTime createdAt;
}

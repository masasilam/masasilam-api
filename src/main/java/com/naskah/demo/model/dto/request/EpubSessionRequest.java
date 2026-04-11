package com.naskah.demo.model.dto.request;

import lombok.Data;

@Data
public class EpubSessionRequest {
    private String  sessionId;
    private Integer durationSeconds;
    private Integer progressPercent;
    private String  cfi;
    private String  deviceType;
}

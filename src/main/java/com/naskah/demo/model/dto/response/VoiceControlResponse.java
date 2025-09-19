package com.naskah.demo.model.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VoiceControlResponse {
    private String recognizedCommand;
    private String action;
    private String result;
    private Boolean success;
    private String error;
    private LocalDateTime processedAt;

    // Navigation results
    private Integer targetPage;
    private String searchQuery;
    private String bookmarkTitle;
}
package com.masasilam.app.model.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReadingProgressCheckResponse {
    private boolean hasProgress;
    private String lastCfi;
    private Double percentageCompleted;
    private LocalDateTime lastReadAt;
}
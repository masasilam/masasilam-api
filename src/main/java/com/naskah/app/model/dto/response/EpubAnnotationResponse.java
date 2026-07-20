package com.naskah.app.model.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EpubAnnotationResponse {
    private Long id;
    private String cfi;
    private String selectedText;
    private String color;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
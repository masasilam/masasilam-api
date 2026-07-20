package com.masasilam.app.model.entity;

import lombok.Data;
import java.time.LocalDateTime;

// EpubAnnotation.java
@Data
public class EpubAnnotation {
    private Long id;
    private Long userId;
    private Long bookId;
    private Long zineId;
    private String cfi;
    private String selectedText;
    private String color;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
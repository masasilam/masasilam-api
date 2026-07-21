package com.masasilam.app.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EpubBookmark {
    private Long id;
    private Long userId;
    private Long bookId;
    private Long zineId;
    private String cfi;
    private String label;
    private LocalDateTime createdAt;
}
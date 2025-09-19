package com.naskah.demo.model.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Discussion {
    private Long id;
    private Long bookId;
    private Long userId;
    private String title;
    private String content;
    private Integer page;
    private String position;
    private Long parentId; // For replies
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
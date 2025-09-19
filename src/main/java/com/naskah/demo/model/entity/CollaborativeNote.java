package com.naskah.demo.model.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CollaborativeNote {
    private Long id;
    private Long bookId;
    private Long authorId;
    private Integer page;
    private String position;
    private String title;
    private String content;
    private String visibility; // PRIVATE, COLLABORATORS_ONLY, PUBLIC
    private Integer editCount;
    private LocalDateTime lastEditedAt;
    private Long lastEditedBy;
    private LocalDateTime createdAt;
}
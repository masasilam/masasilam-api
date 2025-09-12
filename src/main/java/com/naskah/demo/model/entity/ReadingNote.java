package com.naskah.demo.model.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ReadingNote {
    private Long id;
    private Long userId;
    private Long bookId;
    private Integer page;
    private String position;
    private String noteContent;
    private String noteType;
    private String selectedText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

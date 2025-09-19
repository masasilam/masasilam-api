package com.naskah.demo.model.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Bookmark {
    private Long id;
    private Long userId;
    private Long bookId;
    private Integer page;
    private String position;
    private String title;
    private String description;
    private String color;
    private LocalDateTime createdAt;
}
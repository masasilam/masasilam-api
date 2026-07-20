package com.naskah.app.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BlogCategory {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private LocalDateTime createdAt;
}

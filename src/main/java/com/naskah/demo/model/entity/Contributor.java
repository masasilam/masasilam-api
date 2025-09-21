package com.naskah.demo.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Contributor {
    private Long id;
    private String name;
    private String slug;
    private String role;
    private String websiteUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
package com.naskah.app.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Author {
    private Long id;
    private String name;
    private String slug;
    private String birthDate;
    private String deathDate;
    private String birthPlace;
    private String deathPlace;
    private String nationality;
    private String biography;
    private String photoUrl;
    private String photoPath;
    private Integer totalBooks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
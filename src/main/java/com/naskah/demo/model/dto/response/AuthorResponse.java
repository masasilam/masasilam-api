package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AuthorResponse {
    private Long id;
    private String name;
    private String slug;
    private LocalDate birthDate;
    private LocalDate deathDate;
    private String birthPlace;
    private String nationality;
    private String biography;
    private String photoUrl;
    private Integer totalBooks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
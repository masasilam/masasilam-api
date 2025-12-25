package com.naskah.demo.model.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AuthorMetadata {
    private String name;
    private String role;
    private LocalDate birthDate;
    private LocalDate deathDate;
    private String birthPlace;
    private String nationality;
    private String biography;
    private String photoUrl;
}
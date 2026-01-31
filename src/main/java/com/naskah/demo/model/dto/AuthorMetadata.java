package com.naskah.demo.model.dto;

import lombok.Data;

@Data
public class AuthorMetadata {
    private String name;
    private String role;
    private String birthDate;
    private String deathDate;
    private String birthPlace;
    private String nationality;
    private String biography;
    private String photoUrl;
}
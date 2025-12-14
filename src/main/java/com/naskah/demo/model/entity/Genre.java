package com.naskah.demo.model.entity;

import lombok.Data;

import java.time.Instant;

@Data
public class Genre {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String colorHex;
    private String iconName;
    private Boolean isFiction;
    private Integer bookCount;
    private Boolean isActive;
    private Instant createdAt;
}

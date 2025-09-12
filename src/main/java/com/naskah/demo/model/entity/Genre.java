package com.naskah.demo.model.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Genre {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String colorHex;
    private String iconName;
    private Boolean isFiction;
    private Boolean isActive;
    private LocalDateTime createdAt;
}

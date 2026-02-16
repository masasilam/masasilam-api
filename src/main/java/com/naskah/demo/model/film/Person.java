package com.naskah.demo.model.film;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Person entity - Actors, directors, writers, etc.
 */
@Data
public class Person {
    private Long id;
    private String wikidataQid;
    private String slug;
    private String name;
    private String photoUrl;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
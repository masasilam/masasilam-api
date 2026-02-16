package com.naskah.demo.model.film;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Company entity - Production companies, distributors, etc.
 */
@Data
public class Company {
    private Long id;
    private String wikidataQid;
    private String slug;
    private String name;
    private String logoUrl;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
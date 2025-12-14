package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.time.Instant;

@Data
public class GenreResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private Integer bookCount;
    private Instant createdAt;
}

package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ContributorResponse {
    private Long id;
    private String name;
    private String slug;
    private String role;
    private String websiteUrl;
    private Integer totalBooks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

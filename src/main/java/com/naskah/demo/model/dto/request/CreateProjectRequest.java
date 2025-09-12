package com.naskah.demo.model.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Data
public class CreateProjectRequest {
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotBlank(message = "Author is required")
    @Size(max = 255, message = "Author name must not exceed 255 characters")
    private String author;

    @Size(max = 500, message = "Original source must not exceed 500 characters")
    private String originalSource;

    @NotBlank(message = "Difficulty is required")
    @Pattern(regexp = "BEGINNER|EASY|MEDIUM|HARD",
            message = "Invalid difficulty level")
    private String difficulty;

    @NotNull(message = "Language is required")
    private Integer language;

    private List<String> genres;

    @Size(max = 100, message = "Era must not exceed 100 characters")
    private String era;

    @Min(value = 1, message = "Estimated pages must be at least 1")
    @Max(value = 10000, message = "Estimated pages must not exceed 10000")
    private Integer estimatedPages;

    @NotBlank(message = "Project type is required")
    private String projectType;

    private String priority = "MEDIUM";

    // Metadata
    private String metaTitle;
    private String metaDescription;
    private List<String> tags;

    // System field
    private Long currentUserId;

    // OCR Settings
    private String ocrLanguage = "auto";
    private Boolean enableAutoOCR = true;
    private Boolean requireManualReview = false;
}
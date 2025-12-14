package com.naskah.demo.model.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class CompleteEpubMetadata {
    // Basic metadata
    private String title;
    private String subtitle;
    private String description;
    private String publisher;
    private String language;
    private Integer publicationYear;
    private LocalDate publishedAt;
    private String rights;
    private String source;
    private String identifier;

    // Auto-detected
    private String category;
    private String copyrightStatus;

    // Lists
    private List<String> subjects = new ArrayList<>();
    private List<AuthorMetadata> authors = new ArrayList<>();
    private List<ContributorMetadata> contributors = new ArrayList<>();

    // Cover image
    private String coverPath;
    private byte[] coverImageData;
}
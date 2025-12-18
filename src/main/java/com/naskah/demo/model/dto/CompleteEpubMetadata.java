package com.naskah.demo.model.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

import java.time.LocalDateTime;

@Data
public class CompleteEpubMetadata {
    private String title;
    private String subtitle;
    private String description;
    private String publisher;
    private String language;
    private Integer publicationYear;
    private LocalDate publishedAt;
    private LocalDateTime updatedAt;
    private List<AuthorMetadata> authors;
    private List<ContributorMetadata> contributors;
    private List<String> subjects;
    private String category;
    private String copyrightStatus;
    private String source;
    private byte[] coverImageData;
}

package com.masasilam.app.model.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
    private String seriesName;
    private Integer seriesOrder;
    private String seriesDescription;
    private String firstPublished;
    private String firstPublisher;
    private String collectionName;
    private String collectionType;
    private Integer issueNumber;
}
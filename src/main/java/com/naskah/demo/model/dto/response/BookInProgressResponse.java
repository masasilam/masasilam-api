package com.naskah.demo.model.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BookInProgressResponse {
    private Long          bookId;
    private String        bookTitle;
    private String        bookSlug;
    private String        coverImageUrl;
    private String        authorName;
    private Integer       currentChapter;
    private Integer       totalChapters;
    private Double        progressPercentage;
    private LocalDateTime lastReadAt;
    private Integer       remainingMinutes;
}

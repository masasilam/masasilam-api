package com.naskah.app.model.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RecentAnnotationResponse {
    private String        type;
    private String        bookTitle;
    private String        bookSlug;
    private String        content;
    private Integer       chapterNumber;
    private LocalDateTime createdAt;
}
package com.naskah.demo.model.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RecentlyReadResponse {
    private Long          bookId;
    private String        bookTitle;
    private String        bookSlug;
    private String        coverImageUrl;
    private String        authorName;
    private LocalDateTime lastReadAt;
    private String        activityType;
    private Integer       chapterNumber;
    private String        chapterTitle;
}

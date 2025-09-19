package com.naskah.demo.model.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ShareQuoteResponse {
    private String imageUrl;
    private String text;
    private String authorName;
    private String bookTitle;
    private Integer page;
    private String template;
    private LocalDateTime generatedAt;
    private String shareUrl;
}
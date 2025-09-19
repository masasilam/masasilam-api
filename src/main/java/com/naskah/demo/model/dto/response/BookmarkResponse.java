package com.naskah.demo.model.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BookmarkResponse {
    private Long id;
    private Long bookId;
    private Integer page;
    private String position;
    private String title;
    private String description;
    private String color;
    private LocalDateTime createdAt;
}
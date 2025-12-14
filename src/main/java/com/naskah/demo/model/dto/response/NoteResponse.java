package com.naskah.demo.model.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class NoteResponse {
    private Long id;
    private Long bookId;
    private Integer page;
    private String position;
    private String title;
    private String content;
    private String color;
    private Boolean isPrivate;
    private List<String> tags;
    private Integer commentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private ChapterNavigationInfo chapter;
}
package com.masasilam.app.model.dto.monograph;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MonographSitemapItemResponse {
    private String slug;
    private LocalDateTime updatedAt;
}
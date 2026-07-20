package com.naskah.app.model.entity;

import lombok.Data;
import java.time.Instant;

@Data
public class BookSeries {
    private Long id;
    private String name;
    private String description;
    private String slug;
    private String coverImageUrl;
    private Instant createdAt;
}
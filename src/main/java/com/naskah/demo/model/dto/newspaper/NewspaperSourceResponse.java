package com.naskah.demo.model.dto.newspaper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewspaperSourceResponse {
    private Long id;
    private String slug;
    private String name;
    private String nameOriginal;
    private String description;
    private String publisher;
    private String location;
    private Integer yearStart;
    private Integer yearEnd;
    private Integer totalArticles;
    private Long totalViews;
}
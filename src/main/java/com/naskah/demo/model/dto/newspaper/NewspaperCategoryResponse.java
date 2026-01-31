package com.naskah.demo.model.dto.newspaper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewspaperCategoryResponse {
    private String slug;
    private String name;
    private String icon;
    private String description;
    private Integer articleCount;
    private Long totalViews;
}

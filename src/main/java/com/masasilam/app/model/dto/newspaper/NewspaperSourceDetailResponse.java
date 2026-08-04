package com.masasilam.app.model.dto.newspaper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewspaperSourceDetailResponse {
    private Long id;
    private String name;
    private String slug;
    private String logoUrl;
    private String description;
    private String location;
    private String coverImageUrl;
    private Long articleCount;
    private Long editionCount;
    private Integer yearFrom;
    private Integer yearTo;
    private List<Integer> years;
    private List<NewspaperCategoryResponse> genres;
}
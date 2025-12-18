package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class SearchInBookResponse {
    private String query;
    private Integer totalResults;
    private Integer totalChapters;
    private List<ChapterSearchResultResponse> results;
}

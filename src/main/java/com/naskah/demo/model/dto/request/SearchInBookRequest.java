package com.naskah.demo.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SearchInBookRequest {
    @NotBlank(message = "Search query is required")
    @Size(min = 2, max = 200, message = "Query must be between 2 and 200 characters")
    private String query;

    // Filtering
    private Integer chapterFrom;
    private Integer chapterTo;

    // Pagination
    private Integer page = 1;
    private Integer limit = 20;

    // Options
    private Boolean caseSensitive = false;
    private Boolean wholeWord = false;
    private String searchType = "content"; // content, title, both
}

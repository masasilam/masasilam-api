package com.masasilam.app.model.dto.response.social;

import lombok.Data;
import java.util.List;

@Data
public class ReadingListSummaryResponse {
    private Long id;
    private String title;
    private String slug;
    private String coverImageUrl;
    private Integer itemCount;
    private Integer likeCount;
    private List<String> previewCovers; // first 4 item covers
    private List<String> tags;
}
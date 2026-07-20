package com.naskah.app.model.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class AnnotationsSummary {
    private Integer                      totalBookmarks;
    private Integer                      totalHighlights;
    private Integer                      totalNotes;
    private Integer                      totalReviews;
    private List<RecentAnnotationResponse> recentAnnotations;
}

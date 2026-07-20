package com.naskah.app.model.dto.response;

import lombok.Data;

@Data
public class QuickAccessLinks {
    private Integer pendingBookmarks;
    private Integer unreadHighlights;
    private Integer draftNotes;
    private Integer pendingReviews;
}
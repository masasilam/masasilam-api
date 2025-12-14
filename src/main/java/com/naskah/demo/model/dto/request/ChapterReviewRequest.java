package com.naskah.demo.model.dto.request;

import lombok.Data;

@Data
public class ChapterReviewRequest {
    private String comment;
    private Boolean isSpoiler;
}

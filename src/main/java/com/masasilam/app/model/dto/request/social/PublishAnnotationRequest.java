package com.masasilam.app.model.dto.request.social;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PublishAnnotationRequest {
    // Option A: publish from existing epub annotation
    private Long sourceAnnotationId;

    // Option B: create directly
    @NotBlank
    private String entityType;

    @NotNull
    private Long entityId;

    private String entitySlug;   // ✅ Add this
    private String entityTitle;  // ✅ Add this

    private String cfi;
    private String selectedText;
    private String color;
    private String note;
    private String contextBefore;
    private String contextAfter;
    private String chapterLabel;
    private String visibility; // public, followers, private
}
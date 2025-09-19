package com.naskah.demo.model.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class TaggingRequest {
    private List<Long> noteIds; // If empty, tag all notes
    private Boolean autoGenerate = true; // Auto-generate tags or use provided ones
    private List<String> suggestedTags;
}
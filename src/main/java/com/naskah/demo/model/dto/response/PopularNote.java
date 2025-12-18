package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class PopularNote {
    private String theme; // Common theme/topic
    private Integer noteCount;
    private List<String> sampleNotes; // 2-3 example notes
}

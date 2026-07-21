package com.masasilam.app.model.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class PopularNote {
    private String theme;
    private Integer noteCount;
    private List<String> sampleNotes;
}

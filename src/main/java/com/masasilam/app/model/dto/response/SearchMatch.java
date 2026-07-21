package com.masasilam.app.model.dto.response;

import lombok.Data;

@Data
public class SearchMatch {
    private String snippet;
    private Integer position;
    private String contextBefore;
    private String matchText;
    private String contextAfter;
}
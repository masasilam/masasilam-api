package com.naskah.demo.model.dto.response;

import lombok.Data;

@Data
public class SearchMatch {
    private String snippet; // Text snippet with <mark>highlighted</mark> match
    private Integer position; // Character position in chapter
    private String contextBefore; // 50 chars before match
    private String matchText; // The actual matched text
    private String contextAfter; // 50 chars after match
}
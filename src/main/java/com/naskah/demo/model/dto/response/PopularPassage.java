package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class PopularPassage {
    private Long id;
    private Integer chapterNumber;
    private String chapterTitle;
    private String passage;
    private Integer startPosition;
    private Integer endPosition;
    private Integer highlightCount;
    private Double popularityScore;
    private List<String> commonColors; // Most used highlight colors
}
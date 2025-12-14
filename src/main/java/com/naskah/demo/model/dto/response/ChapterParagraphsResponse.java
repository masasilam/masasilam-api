package com.naskah.demo.model.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class ChapterParagraphsResponse {
    private Integer chapterNumber;
    private String chapterTitle;
    private List<String> paragraphs;
    private Integer totalParagraphs;
}

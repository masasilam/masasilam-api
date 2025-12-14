package com.naskah.demo.model.dto;

import com.naskah.demo.model.entity.BookChapter;
import lombok.Data;

import java.util.List;

@Data
public class EpubProcessResult {
    private List<BookChapter> chapters;
    private int totalChapters;
    private long totalWords;
    private String coverImageUrl;
    private String previewText;
}

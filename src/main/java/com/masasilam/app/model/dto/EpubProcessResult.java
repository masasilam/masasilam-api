package com.masasilam.app.model.dto;

import com.masasilam.app.model.entity.BookChapter;
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

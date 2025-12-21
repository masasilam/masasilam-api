package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class ChapterAnnotationsResponse {
    private String chapterSlug;
    private List<BookmarkResponse> bookmarks;
    private List<HighlightResponse> highlights;
    private List<NoteResponse> notes;
}

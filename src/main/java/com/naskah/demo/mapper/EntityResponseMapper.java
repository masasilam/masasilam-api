package com.naskah.demo.mapper;

import com.naskah.demo.model.dto.response.*;
import com.naskah.demo.model.entity.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class EntityResponseMapper {

    public ChapterAudioResponse toChapterAudioResponse(ChapterAudio audio) {
        if (audio == null) return null;

        ChapterAudioResponse response = new ChapterAudioResponse();
        response.setId(audio.getId());
        response.setBookId(audio.getBookId());
        response.setChapterNumber(audio.getChapterNumber());
        response.setAudioUrl(audio.getAudioUrl());
        response.setDuration(audio.getDuration());
        response.setFileSize(audio.getFileSize());
        response.setFormat(audio.getFormat());
        return response;
    }

    public ChapterSummaryResponse toChapterSummaryResponse(BookChapter chapter) {
        if (chapter == null) return null;

        ChapterSummaryResponse response = new ChapterSummaryResponse();
        response.setChapterNumber(chapter.getChapterNumber());
        response.setTitle(chapter.getTitle());
        response.setWordCount(chapter.getWordCount());
        return response;
    }

    public BookmarkResponse toBookmarkResponse(Bookmark bookmark) {
        if (bookmark == null) return null;

        BookmarkResponse response = new BookmarkResponse();
        response.setId(bookmark.getId());
        response.setBookId(bookmark.getBookId());
        response.setChapterNumber(bookmark.getChapterNumber());
        response.setChapterTitle(bookmark.getChapterTitle());
        response.setChapterSlug(bookmark.getChapterSlug());
        response.setPosition(bookmark.getPosition());
        response.setCreatedAt(bookmark.getCreatedAt());
        // HAPUS: title, description, color
        return response;
    }

    public HighlightResponse toHighlightResponse(Highlight highlight) {
        if (highlight == null) return null;

        HighlightResponse response = new HighlightResponse();
        response.setId(highlight.getId());
        response.setBookId(highlight.getBookId());
        response.setChapterNumber(highlight.getChapterNumber());
        response.setChapterTitle(highlight.getChapterTitle());
        response.setChapterSlug(highlight.getChapterSlug());
        response.setStartPosition(highlight.getStartPosition());
        response.setEndPosition(highlight.getEndPosition());
        response.setHighlightedText(highlight.getHighlightedText());
        response.setColor(highlight.getColor());
        response.setCreatedAt(highlight.getCreatedAt());
        response.setUpdatedAt(highlight.getUpdatedAt());
        // HAPUS: color, note
        return response;
    }

    public NoteResponse toNoteResponse(Note note) {
        if (note == null) return null;

        NoteResponse response = new NoteResponse();
        response.setId(note.getId());
        response.setBookId(note.getBookId());
        response.setChapterNumber(note.getChapterNumber());
        response.setChapterTitle(note.getChapterTitle());
        response.setChapterSlug(note.getChapterSlug());
        response.setStartPosition(note.getStartPosition());
        response.setEndPosition(note.getEndPosition());
        response.setContent(note.getContent());
        response.setSelectedText(note.getSelectedText());
        response.setCreatedAt(note.getCreatedAt());
        response.setUpdatedAt(note.getUpdatedAt());
        // HAPUS: title, color, isPrivate
        return response;
    }

    public List<BookmarkResponse> toBookmarkResponseList(List<Bookmark> bookmarks) {
        if (bookmarks == null) return null;
        return bookmarks.stream()
                .map(this::toBookmarkResponse)
                .collect(Collectors.toList());
    }

    public List<HighlightResponse> toHighlightResponseList(List<Highlight> highlights) {
        if (highlights == null) return null;
        return highlights.stream()
                .map(this::toHighlightResponse)
                .collect(Collectors.toList());
    }

    public List<NoteResponse> toNoteResponseList(List<Note> notes) {
        if (notes == null) return null;
        return notes.stream()
                .map(this::toNoteResponse)
                .collect(Collectors.toList());
    }
}
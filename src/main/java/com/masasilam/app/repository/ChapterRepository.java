package com.masasilam.app.repository;

import com.masasilam.app.model.entity.BookChapter;

import java.util.List;

public interface ChapterRepository {
    void insertChapter(BookChapter chapter);
    void updateChapter(BookChapter chapter);
    List<BookChapter> findChaptersByEntityId(Long entityId);
    void deleteChaptersByEntityId(Long entityId);
    void deleteChapterById(Long chapterId);
}
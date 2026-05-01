package com.naskah.demo.repository.impl;

import com.naskah.demo.mapper.BookChapterMapper;
import com.naskah.demo.model.entity.BookChapter;
import com.naskah.demo.repository.ChapterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ChapterRepository untuk Book — delegate ke BookChapterMapper (tabel book_chapters).
 */
@Component("bookChapterRepository")
@RequiredArgsConstructor
public class BookChapterRepository implements ChapterRepository {

    private final BookChapterMapper bookChapterMapper;

    @Override
    public void insertChapter(BookChapter chapter) {
        bookChapterMapper.insertChapter(chapter);
    }

    @Override
    public void updateChapter(BookChapter chapter) {
        bookChapterMapper.updateChapter(chapter);
    }

    @Override
    public BookChapter findChapterByNumber(Long entityId, Integer chapterNumber) {
        return bookChapterMapper.findChapterByNumber(entityId, chapterNumber);
    }

    @Override
    public List<BookChapter> findChaptersByEntityId(Long entityId) {
        return bookChapterMapper.findChaptersByBookId(entityId);
    }

    @Override
    public List<BookChapter> searchInEntity(Long entityId, String query) {
        return bookChapterMapper.searchInBook(entityId, query);
    }

    @Override
    public void deleteChaptersByEntityId(Long entityId) {
        bookChapterMapper.deleteChaptersByBookId(entityId);
    }

    @Override
    public void deleteChapterById(Long chapterId) {
        bookChapterMapper.deleteChapterById(chapterId);
    }
}
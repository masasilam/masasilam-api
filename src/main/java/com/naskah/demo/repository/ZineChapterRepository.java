package com.naskah.demo.repository.impl;

import com.naskah.demo.mapper.ZineChapterMapper;
import com.naskah.demo.model.entity.BookChapter;
import com.naskah.demo.repository.ChapterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ChapterRepository untuk Zine — delegate ke ZineChapterMapper (tabel zine_chapters).
 */
@Component("zineChapterRepository")
@RequiredArgsConstructor
public class ZineChapterRepository implements ChapterRepository {

    private final ZineChapterMapper zineChapterMapper;

    @Override
    public void insertChapter(BookChapter chapter) {
        zineChapterMapper.insertChapter(chapter);
    }

    @Override
    public void updateChapter(BookChapter chapter) {
        zineChapterMapper.updateChapter(chapter);
    }

    @Override
    public BookChapter findChapterByNumber(Long entityId, Integer chapterNumber) {
        return zineChapterMapper.findChapterByNumber(entityId, chapterNumber);
    }

    @Override
    public List<BookChapter> findChaptersByEntityId(Long entityId) {
        return zineChapterMapper.findChaptersByZineId(entityId);
    }

    @Override
    public List<BookChapter> searchInEntity(Long entityId, String query) {
        return zineChapterMapper.searchInZine(entityId, query);
    }

    @Override
    public void deleteChaptersByEntityId(Long entityId) {
        zineChapterMapper.deleteChaptersByZineId(entityId);
    }

    @Override
    public void deleteChapterById(Long chapterId) {
        zineChapterMapper.deleteChapterById(chapterId);
    }
}
package com.masasilam.app.repository.impl;

import com.masasilam.app.mapper.zine.ZineChapterMapper;
import com.masasilam.app.model.entity.BookChapter;
import com.masasilam.app.repository.ChapterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

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
    public List<BookChapter> findChaptersByEntityId(Long entityId) {
        return zineChapterMapper.findChaptersByZineId(entityId);
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
package com.masasilam.app.repository;

import com.masasilam.app.mapper.BookChapterMapper;
import com.masasilam.app.model.entity.BookChapter;
import com.masasilam.app.repository.ChapterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

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
    public List<BookChapter> findChaptersByEntityId(Long entityId) {
        return bookChapterMapper.findChaptersByBookId(entityId);
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
package com.masasilam.app.repository;

import com.masasilam.app.mapper.book.BookChapterMapper;
import com.masasilam.app.model.entity.BookChapter;
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
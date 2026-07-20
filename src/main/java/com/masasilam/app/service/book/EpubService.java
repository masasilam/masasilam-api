package com.masasilam.app.service.book;

import com.masasilam.app.model.dto.EpubProcessResult;
import com.masasilam.app.model.entity.Book;
import com.masasilam.app.repository.ChapterRepository;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface EpubService {
    EpubProcessResult processEpubFile(MultipartFile epubFile, Book book, ChapterRepository chapterRepository) throws IOException;
    EpubProcessResult processEpubFileForUpdate(MultipartFile epubFile, Book book, ChapterRepository chapterRepository) throws IOException;
    void deleteChaptersByEntityId(Long entityId, ChapterRepository chapterRepository);
}
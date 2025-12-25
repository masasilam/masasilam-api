package com.naskah.demo.service.book;

import com.naskah.demo.model.dto.EpubProcessResult;
import com.naskah.demo.model.entity.Book;
import com.naskah.demo.model.entity.BookChapter;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface EpubService {
    EpubProcessResult processEpubFile(MultipartFile epubFile, Book book) throws IOException;

    BookChapter getChapter(Long bookId, Integer chapterNumber);

    List<BookChapter> getAllChapters(Long bookId);

    List<BookChapter> searchInBook(Long bookId, String query);

    void deleteChaptersByBookId(Long bookId);

    EpubProcessResult processEpubFileForUpdate(MultipartFile newFile, Book existingBook) throws IOException;
}

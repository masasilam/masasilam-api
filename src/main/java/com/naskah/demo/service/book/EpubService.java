package com.naskah.demo.service.book;

import com.naskah.demo.model.dto.EpubProcessResult;
import com.naskah.demo.model.entity.Book;
import com.naskah.demo.model.entity.BookChapter;
import com.naskah.demo.repository.ChapterRepository;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface EpubService {

    /**
     * Proses EPUB baru — insert semua chapter.
     *
     * @param chapterRepository repository yang sesuai (book atau zine)
     */
    EpubProcessResult processEpubFile(
            MultipartFile epubFile,
            Book book,
            ChapterRepository chapterRepository) throws IOException;

    /**
     * Proses EPUB untuk update — update chapter yang ada, insert yang baru, hapus yang hilang.
     *
     * @param chapterRepository repository yang sesuai (book atau zine)
     */
    EpubProcessResult processEpubFileForUpdate(
            MultipartFile epubFile,
            Book book,
            ChapterRepository chapterRepository) throws IOException;

    BookChapter getChapter(Long entityId, Integer chapterNumber, ChapterRepository chapterRepository);

    List<BookChapter> getAllChapters(Long entityId, ChapterRepository chapterRepository);

    List<BookChapter> searchInEntity(Long entityId, String query, ChapterRepository chapterRepository);

    void deleteChaptersByEntityId(Long entityId, ChapterRepository chapterRepository);
}
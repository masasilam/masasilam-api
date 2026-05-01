package com.naskah.demo.repository;

import com.naskah.demo.model.entity.BookChapter;

import java.util.List;

/**
 * Abstraction layer untuk chapter storage.
 * Memungkinkan EpubService dipakai untuk Book maupun Zine
 * tanpa coupling ke mapper spesifik.
 */
public interface ChapterRepository {

    /**
     * Insert chapter baru, mengisi chapter.id setelah insert (useGeneratedKeys).
     */
    void insertChapter(BookChapter chapter);

    /**
     * Update chapter yang sudah ada.
     */
    void updateChapter(BookChapter chapter);

    /**
     * Cari chapter berdasarkan nomor urut dalam satu entitas (book/zine).
     */
    BookChapter findChapterByNumber(Long entityId, Integer chapterNumber);

    /**
     * Ambil semua chapter milik satu entitas, urut berdasarkan chapter_number.
     */
    List<BookChapter> findChaptersByEntityId(Long entityId);

    /**
     * Full-text search di dalam chapter satu entitas.
     */
    List<BookChapter> searchInEntity(Long entityId, String query);

    /**
     * Hapus semua chapter milik satu entitas.
     */
    void deleteChaptersByEntityId(Long entityId);

    /**
     * Hapus satu chapter berdasarkan ID-nya.
     */
    void deleteChapterById(Long chapterId);
}
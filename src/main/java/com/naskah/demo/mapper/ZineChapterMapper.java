package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.BookChapter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * MyBatis Mapper untuk tabel zine_chapters.
 * Menggunakan BookChapter sebagai entity (struktur kolom identik),
 * hanya berbeda tabel dan FK (zine_id vs book_id).
 */
@Mapper
public interface ZineChapterMapper {

    /**
     * Insert chapter baru ke zine_chapters.
     * bookId field pada entity digunakan sebagai zine_id.
     */
    void insertChapter(BookChapter chapter);

    /**
     * Update chapter yang sudah ada di zine_chapters.
     */
    void updateChapter(BookChapter chapter);

    /**
     * Cari chapter berdasarkan zine_id dan chapter_number.
     */
    BookChapter findChapterByNumber(@Param("zineId") Long zineId,
                                    @Param("chapterNumber") Integer chapterNumber);

    /**
     * Ambil semua chapter milik satu zine, urut berdasarkan chapter_number.
     */
    List<BookChapter> findChaptersByZineId(@Param("zineId") Long zineId);

    /**
     * Full-text search di dalam chapter satu zine.
     */
    List<BookChapter> searchInZine(@Param("zineId") Long zineId,
                                   @Param("query") String query);

    /**
     * Hapus semua chapter milik satu zine.
     */
    void deleteChaptersByZineId(@Param("zineId") Long zineId);

    /**
     * Hapus satu chapter berdasarkan ID-nya.
     */
    void deleteChapterById(@Param("id") Long id);
}
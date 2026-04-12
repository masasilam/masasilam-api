package com.naskah.demo.model.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReadingSession {

    private Long          id;
    private Long          userId;
    private Long          bookId;
    private String        sessionId;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer       totalDurationSeconds;

    // ── Chapter-specific fields ──────────────────────────────────────────────
    // Untuk sesi EPUB, ketiga field ini diisi dari TOC (chapterIndex + 1).
    // Gunakan sessionType untuk membedakan makna nilainya.
    private Integer startChapter;
    private Integer endChapter;
    private Integer chaptersRead;

    // ── Penanda eksplisit tipe sesi ──────────────────────────────────────────
    // "EPUB"    → sesi dari EpubReaderPage
    // "CHAPTER" → sesi dari ChapterReaderPage
    private String  sessionType;

    private Double  completionDelta;
    private Integer totalInteractions;
    private String  deviceType;

    // ── FIX: ditambahkan agar sesuai kolom DB dan ReadingSessionMapper.xml ───
    // Nullable — frontend tidak mengirim deviceId, nilai default null.
    private String  deviceId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
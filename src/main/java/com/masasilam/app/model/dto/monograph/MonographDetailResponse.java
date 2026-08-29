package com.masasilam.app.model.dto.monograph;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class MonographDetailResponse {
    private Long id;
    private String slug;
    private String title;
    private String subtitle;
    private String newspaperSourceName;
    private String newspaperSourceSlug;
    private LocalDate editionDate;
    private String editionDateFormatted;
    private String editorialNote;
    private String colophon;
    private String centralQuestion;
    private String methodNote;
    private String coverImageUrl;
    private String citationFormatTemplate;
    private String status;
    private Integer totalArticles;
    private Integer inBookCount;
    private Integer appendixCount;
    private Integer wordCount;
    private Integer readingTimeMinutes;
    private Integer viewCount;
    private List<MonographChapterResponse> chapters;
    private List<MonographEntryResponse> counterpoints;
    private List<MonographEntryResponse> interludes;
    private List<MonographEntryResponse> peripheralDocuments;
    private List<MonographEntryResponse> appendices;
    private List<MonographEntryResponse> carryForwardPool;
    private List<MonographEntryResponse> provenanceRegister;
    private List<MonographCriticalNoteResponse> criticalNotes;
    private List<MonographIndexEntryResponse> nameIndex;
    private List<MonographIndexEntryResponse> placeIndex;
    private List<MonographIndexEntryResponse> orgIndex;
    private List<MonographGlossaryResponse> glossary;
}
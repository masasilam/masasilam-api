package com.masasilam.app.mapper.monograph;

import com.masasilam.app.model.dto.monograph.*;
import com.masasilam.app.model.entity.monograph.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MonographMapper {
    void insertMonograph(Monograph monograph);
    void updateMonograph(Monograph monograph);
    void updateMonographStatus(@Param("id") Long id, @Param("status") String status, @Param("publishedAt") LocalDateTime publishedAt);
    void softDeleteMonograph(@Param("id") Long id);
    Monograph findById(@Param("id") Long id);
    Monograph findBySlug(@Param("slug") String slug);
    boolean existsBySlug(@Param("slug") String slug);
    boolean existsBySlugExcluding(@Param("slug") String slug, @Param("excludeId") Long excludeId);
    MonographDetailResponse getMonographBaseById(@Param("id") Long id);
    List<MonographResponse> getMonographs(@Param("offset") int offset, @Param("limit") int limit, @Param("sortField") String sortField, @Param("sortOrder") String sortOrder, @Param("criteria") MonographSearchCriteria criteria);
    int countMonographs(@Param("criteria") MonographSearchCriteria criteria);
    List<MonographResponse> getLatestMonographs(@Param("limit") int limit);
    List<MonographSitemapItemResponse> getMonographsForSitemap();
    void insertChapter(MonographChapter chapter);
    void deleteChaptersByMonograph(@Param("monographId") Long monographId);
    List<MonographChapterResponse> getChaptersByMonograph(@Param("monographId") Long monographId);
    void insertEntry(MonographEntry entry);
    void deleteEntriesByMonograph(@Param("monographId") Long monographId);
    List<MonographEntryResponse> getAllEntries(@Param("monographId") Long monographId);
    void insertCriticalNote(MonographCriticalNote note);
    void deleteCriticalNotesByMonograph(@Param("monographId") Long monographId);
    List<MonographCriticalNoteResponse> getCriticalNotesByMonograph(@Param("monographId") Long monographId);
    void insertGlossaryTerm(MonographGlossaryTerm term);
    void deleteGlossaryByMonograph(@Param("monographId") Long monographId);
    List<MonographGlossaryResponse> getGlossaryByMonograph(@Param("monographId") Long monographId);
    void insertIndexEntry(MonographIndexEntry entry);
    void insertIndexRef(@Param("indexEntryId") Long indexEntryId, @Param("articleId") Long articleId);
    void deleteIndexByMonograph(@Param("monographId") Long monographId);
    List<MonographIndexEntryRow> getIndexEntriesByType(@Param("monographId") Long monographId, @Param("indexType") String indexType);
    int countTotalEntries(@Param("monographId") Long monographId);
    int countChapters(@Param("monographId") Long monographId);
    int countChaptersWithoutEntries(@Param("monographId") Long monographId);
    int countInactiveEntryArticles(@Param("monographId") Long monographId);
    boolean hasViewByHash(@Param("viewerHash") String viewerHash);
    void insertMonographView(MonographView view);
    void incrementViewCount(@Param("monographId") Long monographId);
    String getSourceNameById(@Param("sourceId") Long sourceId);
    List<MonographScaffoldArticle> getArticlesForScaffold(@Param("sourceId") Long sourceId, @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);
    String getSourceSlugById(@Param("sourceId") Long sourceId);
    MonographAdjacentResponse.MonographNeighbor getPreviousMonograph(@Param("sourceId") Long sourceId, @Param("editionDate") LocalDate editionDate, @Param("id") Long id);
    MonographAdjacentResponse.MonographNeighbor getNextMonograph(@Param("sourceId") Long sourceId, @Param("editionDate") LocalDate editionDate, @Param("id") Long id);
}
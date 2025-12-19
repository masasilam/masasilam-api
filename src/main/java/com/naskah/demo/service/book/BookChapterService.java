package com.naskah.demo.service.book;

import com.naskah.demo.model.dto.request.*;
import com.naskah.demo.model.dto.response.*;

import java.util.List;

public interface BookChapterService {
    DataResponse<ChapterReadingResponse> readChapterBySlugPath(String bookSlug, String slugPath);
    DataResponse<List<ChapterSummaryResponse>> getAllChaptersSummary(String slug);
    DataResponse<ChapterProgressResponse> saveChapterProgress(String slug, Integer chapterNumber, ChapterProgressRequest request);
    DataResponse<ChapterTextResponse> getChapterTextForTTS(String slug, Integer chapterNumber);
    DataResponse<ChapterParagraphsResponse> getChapterParagraphs(String slug, Integer chapterNumber);
    DataResponse<BookmarkResponse> addChapterBookmark(String slug, Integer chapterNumber, ChapterBookmarkRequest request);
    DataResponse<Void> deleteChapterBookmark(String slug, Integer chapterNumber, Long bookmarkId);
    DataResponse<HighlightResponse> addChapterHighlight(String slug, Integer chapterNumber, ChapterHighlightRequest request);
    DataResponse<Void> deleteChapterHighlight(String slug, Integer chapterNumber, Long highlightId);
    DataResponse<NoteResponse> addChapterNote(String slug, Integer chapterNumber, ChapterNoteRequest request);
    DataResponse<Void> deleteChapterNote(String slug, Integer chapterNumber, Long noteId);
    DataResponse<ChapterAnnotationsResponse> getMyChapterAnnotations(String slug);
    DataResponse<ChapterRatingResponse> rateChapter(String slug, Integer chapterNumber, ChapterRatingRequest request);
    DataResponse<ChapterRatingSummaryResponse> getChapterRatingSummary(String slug, Integer chapterNumber);
    DataResponse<Void> deleteChapterRating(String slug, Integer chapterNumber);
    DataResponse<List<ChapterReviewResponse>> getChapterReviews(String slug, Integer chapterNumber, int page, int limit);
    DataResponse<ChapterReviewResponse> addChapterReview(String slug, Integer chapterNumber, ChapterReviewRequest request);
    DataResponse<ChapterReviewResponse> replyToChapterReview(String slug, Integer chapterNumber, Long reviewId, ChapterReplyRequest request);
    DataResponse<Void> likeChapterReview(String slug, Integer chapterNumber, Long reviewId);
    DataResponse<Void> unlikeChapterReview(String slug, Integer chapterNumber, Long reviewId);
    DataResponse<Void> startReading(String slug, StartReadingRequest request);
    DataResponse<Void> endReading(String slug, EndReadingRequest request);
//    DataResponse<Void> readingHeartbeat(String slug, ReadingHeartbeatRequest request);
    DataResponse<ReadingHistoryResponse> getReadingHistory(String slug);
    DataResponse<UserReadingPatternResponse> getUserReadingPattern(String slug);
    DataResponse<SearchInBookResponse> searchInBook(String slug, SearchInBookRequest request);
    DataResponse<List<SearchHistoryResponse>> getSearchHistory(String slug, int limit);
    DataResponse<ExportAnnotationsResponse> exportAnnotations(String slug, ExportAnnotationsRequest request);
    DataResponse<ExportAnnotationsResponse> getExportStatus(Long exportId);
    String getExportDownloadUrl(Long exportId);
    DataResponse<ExportHistoryResponse> getExportHistory(int page, int limit);
    DataResponse<Void> deleteExport(Long exportId);
    DataResponse<UserBookDataResponse> getMyBookData(String slug);
    DataResponse<BookAnalyticsResponse> getBookAnalytics(String slug, String dateFrom, String dateTo);
    DataResponse<List<ChapterAnalyticsResponse>> getChaptersAnalytics(String slug);
    DataResponse<ChapterStatsResponse> getChapterStats(String slug, Integer chapterNumber);
}
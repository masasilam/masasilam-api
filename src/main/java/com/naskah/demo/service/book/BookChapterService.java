package com.naskah.demo.service.book;

import com.naskah.demo.model.dto.request.*;
import com.naskah.demo.model.dto.response.*;

import java.util.List;

public interface BookChapterService {
    DataResponse<ChapterReadingResponse> readChapter(String slug, Integer chapterNumber);
    DataResponse<List<ChapterSummaryResponse>> getAllChaptersSummary(String slug);
    DataResponse<ChapterTextResponse> getChapterTextForTTS(String slug, Integer chapterNumber);
    DataResponse<ChapterParagraphsResponse> getChapterParagraphs(String slug, Integer chapterNumber);
    DataResponse<ChapterProgressResponse> saveChapterProgress(String slug, Integer chapterNumber, ChapterProgressRequest request);
    DataResponse<ChapterAnnotationsResponse> getMyChapterAnnotations(String slug);
    DataResponse<BookmarkResponse> addChapterBookmark(String slug, Integer chapterNumber, ChapterBookmarkRequest request);
    DataResponse<HighlightResponse> addChapterHighlight(String slug, Integer chapterNumber, ChapterHighlightRequest request);
    DataResponse<NoteResponse> addChapterNote(String slug, Integer chapterNumber, ChapterNoteRequest request);
    DataResponse<List<ChapterReviewResponse>> getChapterReviews(String slug, Integer chapterNumber, int page, int limit);
    DataResponse<ChapterReviewResponse> addChapterReview(String slug, Integer chapterNumber, ChapterReviewRequest request);
    DataResponse<ChapterReviewResponse> replyToChapterReview(String slug, Integer chapterNumber, Long reviewId, ChapterReplyRequest request);
    DataResponse<Void> likeChapterReview(String slug, Integer chapterNumber, Long reviewId);
    DataResponse<Void> unlikeChapterReview(String slug, Integer chapterNumber, Long reviewId);
    DataResponse<ChapterStatsResponse> getChapterStats(String slug, Integer chapterNumber);
    DataResponse<Void> deleteChapterBookmark(String slug, Integer chapterNumber, Long bookmarkId);
    DataResponse<Void> deleteChapterHighlight(String slug, Integer chapterNumber, Long highlightId);
    DataResponse<Void> deleteChapterNote(String slug, Integer chapterNumber, Long noteId);
    DataResponse<ChapterReadingResponse> readChapterBySlugPath(String bookSlug, String chapterPath);
}

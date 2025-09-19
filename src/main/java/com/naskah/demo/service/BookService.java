package com.naskah.demo.service;

import com.naskah.demo.model.dto.request.*;
import com.naskah.demo.model.dto.response.*;
import com.naskah.demo.model.entity.Book;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface BookService {

    // Existing methods
    DataResponse<BookResponse> createBook(BookRequest request);
    DataResponse<BookResponse> getBookDetailBySlug(String slug);
    DataResponse<ReadingResponse> startReading(String slug);
    DatatableResponse<BookResponse> getPaginatedBooks(int page, int limit, String sortField, String sortOrder,
                                                      String searchTitle, Long seriesId, Long genreId, Long subGenreId);
    ResponseEntity<byte[]> downloadBookAsBytes(String slug);
//    DataResponse<Book> update(String id, Book book, MultipartFile file) throws IOException;
//    DefaultResponse delete(String id) throws IOException;

    // ============ NEW METHODS FOR ALL 30 FEATURES ============

    // 1-2. Reading Progress Management
    DataResponse<ReadingProgressResponse> saveReadingProgress(String slug, ProgressRequest request);
    DataResponse<ReadingProgressResponse> getReadingProgress(String slug);

    // 3-4. Bookmark Management
    DataResponse<BookmarkResponse> addBookmark(String slug, BookmarkRequest request);
    DataResponse<List<BookmarkResponse>> getBookmarks(String slug);

    // 5. Search in Book
    DataResponse<SearchResultResponse> searchInBook(String slug, String query, int page, int limit);

    // 6-7. Highlight Management
    DataResponse<HighlightResponse> addHighlight(String slug, HighlightRequest request);
    DataResponse<List<HighlightResponse>> getHighlights(String slug);

    // 8-9. Notes Management
    DataResponse<NoteResponse> addNote(String slug, NoteRequest request);
    DataResponse<List<NoteResponse>> getNotes(String slug);

    // 10. Export Features
    DataResponse<ExportResponse> exportHighlightsAndNotes(String slug, String format);

    // 11-13. Translation Features
    DataResponse<TranslationResponse> translateText(String slug, TranslationRequest request);
//    DataResponse<DualLanguageResponse> getDualLanguageView(String slug, String targetLanguage, int page);
//    DataResponse<TranslatedHighlightResponse> translateHighlight(String slug, TranslateHighlightRequest request);
//
//    // 14-15. Social Features
//    DataResponse<ReactionResponse> addReaction(String slug, ReactionRequest request);
//    DataResponse<List<DiscussionResponse>> getDiscussions(String slug, int page, int limit);
//    DataResponse<DiscussionResponse> addDiscussion(String slug, DiscussionRequest request);
//
//    // 16-17. Audio Features
//    DataResponse<TTSResponse> generateTextToSpeech(String slug, TTSRequest request);
//    DataResponse<AudioSyncResponse> syncAudioWithText(String slug, AudioSyncRequest request);
//
//    // 18. Voice Notes
//    DataResponse<VoiceNoteResponse> addVoiceNote(String slug, MultipartFile audioFile, int page, String position);
//    DataResponse<List<VoiceNoteResponse>> getVoiceNotes(String slug);
//
//    // 19. Vocabulary Builder
//    DataResponse<VocabularyResponse> extractVocabulary(String slug, VocabularyRequest request);
//
//    // 20-21. AI Features
//    DataResponse<SummaryResponse> generateChapterSummary(String slug, SummaryRequest request);
//    DataResponse<QAResponse> askQuestionAboutBook(String slug, QARequest request);
//
//    // 22. Comment System
//    DataResponse<CommentResponse> addCommentToNote(String slug, Long noteId, CommentRequest request);
//
//    // 23. Quote Sharing
//    DataResponse<ShareQuoteResponse> generateShareableQuote(String slug, ShareQuoteRequest request);
//
//    // 24-25. Analytics and Suggestions
//    DataResponse<HighlightTrendsResponse> getHighlightTrends(String slug);
//    DataResponse<List<BookmarkSuggestionResponse>> getBookmarkSuggestions(String slug);
//
//    // 26. Interactive Learning
//    DataResponse<QuizResponse> generateChapterQuiz(String slug, QuizRequest request);
//
//    // 27-28. Smart AI Features
//    DataResponse<AIHighlightResponse> generateAIHighlights(String slug, AIHighlightRequest request);
//    DataResponse<TaggingResponse> autoTagNotes(String slug, TaggingRequest request);
//
//    // 29. Voice Control
//    DataResponse<VoiceControlResponse> processVoiceCommand(String slug, MultipartFile audioFile);
//
//    // 30. Collaborative Features
//    DataResponse<CollaborativeNoteResponse> createCollaborativeNote(String slug, CollaborativeNoteRequest request);
//    DataResponse<List<CollaborativeNoteResponse>> getCollaborativeNotes(String slug);
}
package com.naskah.demo.service.book;

import com.naskah.demo.model.dto.BookSearchCriteria;
import com.naskah.demo.model.dto.request.*;
import com.naskah.demo.model.dto.response.*;
import com.naskah.demo.model.entity.Book;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface BookService {

    // Basic book operations
    DataResponse<BookResponse> createBook(BookRequest request);
    DataResponse<BookResponse> getBookDetailBySlug(String slug);
    ResponseEntity<byte[]> downloadBookAsBytes(String slug);
    DataResponse<Book> update(Long id, Book book, MultipartFile file) throws IOException;
    DefaultResponse delete(Long id) throws IOException;

    // Search
    DataResponse<SearchResultResponse> searchInBook(String slug, String query, int page, int limit);

    // Genre operations
    DataResponse<List<GenreResponse>> getAllGenres(boolean includeBookCount);

    // Author operations
    DatatableResponse<AuthorResponse> getAllAuthors(int page, int limit, String search, String sortBy);

    // Contributor operations
    DatatableResponse<ContributorResponse> getAllContributors(int page, int limit, String role, String search);

    DatatableResponse<BookResponse> getPaginatedBooks(int page, int limit, String sortField, String sortOrder, BookSearchCriteria criteria);

//    // ============ RATING OPERATIONS ============
//    DataResponse<ReactionResponse> addOrUpdateRating(String slug, RatingRequest request);
//    DataResponse<Void> deleteRating(String slug);
//
//    // ============ REVIEW/COMMENT OPERATIONS ============
//    DataResponse<List<ReactionResponse>> getReviews(String slug, int page, int limit);
//    DataResponse<ReactionResponse> addReview(String slug, ReviewRequest request);
//    DataResponse<ReactionResponse> updateReview(String slug, ReviewRequest request);
//    DataResponse<Void> deleteReview(String slug);
//
//    // ============ REPLY OPERATIONS ============
//    DataResponse<ReactionResponse> addReply(String slug, Long parentId, ReplyRequest request);
//    DataResponse<ReactionResponse> updateReply(String slug, Long replyId, ReplyRequest request);
//    DataResponse<Void> deleteReply(String slug, Long replyId);
//
//    // ============ FEEDBACK OPERATIONS ============
//    DataResponse<ReactionResponse> addOrUpdateFeedback(String slug, Long reviewId, FeedbackRequest request);
//    DataResponse<Void> deleteFeedback(String slug, Long reviewId);
}
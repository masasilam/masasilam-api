package com.masasilam.app.model.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class FilmReviewResponse {
    private Long          id;
    private Long          filmId;
    private Long          userId;
    private String        username;
    private String        userPhotoUrl;
    private String        title;
    private String        content;
    private Integer       helpfulCount;
    private Integer       notHelpfulCount;
    private Integer       replyCount;

    /** true jika current user adalah penulis review ini */
    private Boolean isOwner;

    /**
     * Feedback current user terhadap review ini.
     * null  = belum memberi feedback
     * true  = menandai "helpful"
     * false = menandai "not helpful"
     */
    private Boolean currentUserFeedback;

    /** Semua balasan (reply) untuk review ini */
    private List<FilmReviewReplyResponse> replies;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
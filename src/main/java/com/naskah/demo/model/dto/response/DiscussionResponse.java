package com.naskah.demo.model.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class DiscussionResponse {
    private Long id;
    private String title;
    private String content;
    private String authorName;
    private Integer page;
    private String position;
    private Integer replyCount;
    private Integer likeCount;
    private Boolean isLikedByUser;
    private LocalDateTime createdAt;
    private List<DiscussionReply> replies;

    @Data
    public static class DiscussionReply {
        private Long id;
        private String content;
        private String authorName;
        private Integer likeCount;
        private Boolean isLikedByUser;
        private LocalDateTime createdAt;
    }
}
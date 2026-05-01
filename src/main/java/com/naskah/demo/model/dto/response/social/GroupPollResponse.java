package com.naskah.demo.model.dto.response.social;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class GroupPollResponse {
    private Long id;
    private Long groupId;
    private Long createdBy;
    private String creatorUsername;
    private String question;
    private List<PollOptionResponse> options;
    private LocalDateTime endsAt;
    private Boolean isClosed;
    private Integer totalVotes;
    private Integer myVoteOptionId;  // null if not voted
    private LocalDateTime createdAt;

    @Data
    public static class PollOptionResponse {
        private Integer id;
        private String text;
        private Integer voteCount;
        private Double percentage;
    }
}
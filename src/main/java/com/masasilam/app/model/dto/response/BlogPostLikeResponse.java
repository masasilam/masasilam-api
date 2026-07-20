package com.masasilam.app.model.dto.response;

import lombok.Data;

@Data
public class BlogPostLikeResponse {
    private Long blogPostId;
    private Boolean isLiked;
    private Long totalLikes;
}

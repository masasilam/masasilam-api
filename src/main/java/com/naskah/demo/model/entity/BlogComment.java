package com.naskah.demo.model.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Entity BlogComment — disesuaikan dengan struktur tabel aktual:
 *
 * blog_comments:
 *   id                BIGINT PK
 *   post_id           BIGINT FK → blog_posts.id   (BUKAN blog_post_id)
 *   user_id           BIGINT FK → users.id
 *   parent_comment_id BIGINT nullable              (BUKAN parent_id)
 *   content           TEXT
 *   is_approved       BOOLEAN
 *   like_count        INT
 *   created_at        TIMESTAMP
 *
 * TIDAK ADA: updated_at
 */
@Data
public class BlogComment {

    private Long id;

    /** Mapped dari kolom post_id di DB */
    private Long blogPostId;

    private Long userId;

    private String content;

    /** Mapped dari kolom parent_comment_id di DB */
    private Long parentId;

    private Boolean isApproved;

    private Long likeCount;

    private LocalDateTime createdAt;

    // Tidak ada updatedAt — kolom tidak ada di tabel
}
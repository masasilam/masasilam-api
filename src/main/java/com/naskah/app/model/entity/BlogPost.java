package com.naskah.app.model.entity;

import com.naskah.app.model.enums.BlogPostStatus;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Entity BlogPost — disesuaikan dengan struktur tabel aktual:
 *
 * blog_posts:
 *   id                    BIGINT PK
 *   user_id               BIGINT FK → users.id      (authorId di Java)
 *   title                 VARCHAR
 *   slug                  VARCHAR UNIQUE
 *   content               TEXT
 *   excerpt               TEXT
 *   featured_image_url    VARCHAR
 *   status                VARCHAR (DRAFT/PUBLISHED/SCHEDULED)
 *   is_featured           BOOLEAN
 *   view_count            INT
 *   comment_count         INT
 *   like_count            INT
 *   reading_time_minutes  INT
 *   published_at          TIMESTAMP
 *   created_at            TIMESTAMP
 *   updated_at            TIMESTAMP
 *   scheduled_at          TIMESTAMP
 */
@Data
public class BlogPost {

    private Long id;

    /** Mapped dari kolom user_id di DB */
    private Long authorId;

    private String title;
    private String slug;
    private String content;
    private String excerpt;
    private String source;

    /** Mapped dari kolom featured_image_url di DB */
    private String featuredImage;

    private BlogPostStatus status;

    private Boolean isFeatured;

    private Long viewCount;
    private Long likeCount;
    private Long commentCount;
    private Integer readingTimeMinutes;

    private LocalDateTime scheduledAt;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
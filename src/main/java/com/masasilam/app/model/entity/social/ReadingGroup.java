package com.masasilam.app.model.entity.social;

import lombok.Data;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
public class ReadingGroup {
    private Long id;
    private Long ownerId;
    private String name;
    private String slug;
    private String description;
    private String coverImageUrl;
    private String groupType;   // public, private, invite_only
    private String focusType;   // BOOK, ZINE, FILM, NEWSPAPER, mixed
    private Integer memberCount;
    private Integer maxMembers;
    private String currentReadEntityType;
    private Long currentReadEntityId;
    private String currentReadTitle;
    private LocalDate currentReadStart;
    private LocalDate currentReadEnd;
    private String tags;
    private String rules;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
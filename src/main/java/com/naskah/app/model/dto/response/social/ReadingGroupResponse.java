package com.naskah.app.model.dto.response.social;

import lombok.Data;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Data
public class ReadingGroupResponse {
    private Long id;
    private Long ownerId;
    private String ownerUsername;
    private String ownerPhoto;
    private String name;
    private String slug;
    private String description;
    private String coverImageUrl;
    private String groupType;
    private String focusType;
    private Integer memberCount;
    private Integer maxMembers;
    private List<String> tags;
    private String rules;

    // Current read
    private String currentReadEntityType;
    private Long currentReadEntityId;
    private String currentReadTitle;
    private String currentReadCover;
    private LocalDate currentReadStart;
    private LocalDate currentReadEnd;

    // Current user context
    private Boolean isMember;
    private String myRole;   // owner, moderator, member, null
    private Boolean hasPendingRequest;

    // Previews
    private List<GroupMemberResponse> recentMembers;
    private GroupReadingScheduleResponse activeSchedule;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
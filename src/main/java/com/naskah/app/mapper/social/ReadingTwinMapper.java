package com.naskah.app.mapper.social;

import com.naskah.app.model.entity.social.ReadingTwin;
import com.naskah.app.model.dto.response.social.ReadingTwinResponse;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ReadingTwinMapper {
    void upsert(ReadingTwin twin);
    ReadingTwin findByUsers(@Param("userIdA") Long userIdA, @Param("userIdB") Long userIdB);
    List<ReadingTwinResponse> findTwinsForUser(@Param("userId") Long userId,
                                               @Param("currentUserId") Long currentUserId,
                                               @Param("offset") int offset, @Param("limit") int limit);
    int countTwinsForUser(Long userId);

    // For calculation job
    List<Long> findUsersToCompare(Long userId); // returns user IDs that read same content
    void deleteStaleFor(Long userId);
}
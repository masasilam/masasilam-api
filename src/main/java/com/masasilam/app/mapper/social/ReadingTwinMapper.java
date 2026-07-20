package com.masasilam.app.mapper.social;

import com.masasilam.app.model.entity.social.ReadingTwin;
import com.masasilam.app.model.dto.response.social.ReadingTwinResponse;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ReadingTwinMapper {
    void upsert(ReadingTwin twin);
    ReadingTwin findByUsers(@Param("userIdA") Long userIdA, @Param("userIdB") Long userIdB);
    List<ReadingTwinResponse> findTwinsForUser(@Param("userId") Long userId, @Param("currentUserId") Long currentUserId, @Param("offset") int offset, @Param("limit") int limit);
    int countTwinsForUser(Long userId);
    List<Long> findUsersToCompare(Long userId);
    void deleteStaleFor(Long userId);
}
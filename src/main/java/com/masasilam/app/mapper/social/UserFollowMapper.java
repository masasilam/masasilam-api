package com.masasilam.app.mapper.social;

import com.masasilam.app.model.entity.social.UserFollow;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserFollowMapper {
    void insert(UserFollow follow);
    void delete(@Param("followerId") Long followerId, @Param("followingId") Long followingId);
    UserFollow findByFollowerAndFollowing(@Param("followerId") Long followerId, @Param("followingId") Long followingId);
    boolean isFollowing(@Param("followerId") Long followerId, @Param("followingId") Long followingId);
    int countMutualFollows(@Param("userIdA") Long userIdA, @Param("userIdB") Long userIdB);
}
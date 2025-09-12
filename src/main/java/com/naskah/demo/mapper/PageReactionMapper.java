package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.PageReaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PageReactionMapper {
    PageReaction getPageReactionByUserAndPage(@Param("userId") Long userId, @Param("id") Long id);
    void deletePageReaction(@Param("id") Long id);
    void updatePageReaction(@Param("existingReaction") PageReaction existingReaction);
    void insertPageReaction(@Param("newReaction") PageReaction newReaction);
    Long getPageReactionCount(@Param("pageId") Long pageId);
    Long getUserTotalPageReactionsReceived(@Param("userId") Long userId);
}
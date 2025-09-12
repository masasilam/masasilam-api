package com.naskah.demo.mapper;

import com.naskah.demo.model.dto.response.ReactionSummary;
import com.naskah.demo.model.entity.ProjectReaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProjectReactionMapper {
    ProjectReaction getProjectReactionByUserAndProject(@Param("userId") Long userId, @Param("projectId") Long projectId);
    void deleteProjectReaction(@Param("id") Long id);
    void updateProjectReaction(@Param("existingReaction") ProjectReaction existingReaction);
    void insertProjectReaction(@Param("newReaction") ProjectReaction newReaction);
    Long getProjectReactionCount(@Param("projectId") Long projectId);
    List<ReactionSummary> getProjectReactionBreakdown(@Param("projectId") Long projectId);
    Long getUserTotalProjectReactionsReceived(@Param("userId") Long userId);
}
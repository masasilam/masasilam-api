package com.naskah.demo.mapper;

import com.naskah.demo.model.dto.response.ProjectCommentResponse;
import com.naskah.demo.model.entity.ProjectComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProjectCommentMapper {
    void insertProjectComment(ProjectComment comment);
    ProjectComment getProjectCommentById(@Param("parentCommentId") Long parentCommentId);
    ProjectCommentResponse getProjectCommentWithUserInfo(@Param("id") Long id);
    List<ProjectCommentResponse> getProjectCommentsWithUserInfo(@Param("id") Long id, @Param("offset") int offset, @Param("limit") int limit, @Param("sortType") String sortType);
    Long getProjectCommentCount(@Param("id") Long id);
    int getCommentReplyCount(@Param("id") Long id);
    List<ProjectCommentResponse> getCommentReplies(@Param("id") Long id, @Param("i") int i, @Param("i1") int i1);
    Long getUserTotalProjectCommentsReceived(@Param("userId") Long userId);
}
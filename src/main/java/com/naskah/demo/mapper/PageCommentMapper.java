package com.naskah.demo.mapper;

import com.naskah.demo.model.dto.response.PageCommentResponse;
import com.naskah.demo.model.entity.PageComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PageCommentMapper {
    void insertPageComment(PageComment comment);
    PageComment getPageCommentById(@Param("parentCommentId") Long parentCommentId);
    PageCommentResponse getPageCommentWithUserInfo(@Param("id") Long id);
    List<PageCommentResponse> getPageCommentsWithUserInfo(@Param("id") Long id, @Param("offset") int offset, @Param("limit") int limit, @Param("sortType") String sortType);
    int getPageCommentReplyCount(@Param("id") Long id);
    List<PageCommentResponse> getPageCommentReplies(@Param("id") Long id, @Param("i") int i, @Param("i1") int i1);
    Long getPageCommentCount(@Param("pageId") Long pageId);
    Long getUserTotalPageCommentsReceived(@Param("userId") Long userId);
}
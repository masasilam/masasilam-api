package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.Discussion;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DiscussionMapper {

    @Insert("INSERT INTO discussions (book_id, user_id, title, content, page, position, parent_id, created_at) " +
            "VALUES (#{bookId}, #{userId}, #{title}, #{content}, #{page}, #{position}, #{parentId}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertDiscussion(Discussion discussion);

    @Select("SELECT * FROM discussions WHERE book_id = #{bookId} AND parent_id IS NULL " +
            "ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<Discussion> findDiscussionsByBook(@Param("bookId") Long bookId, @Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT * FROM discussions WHERE parent_id = #{parentId} ORDER BY created_at ASC")
    List<Discussion> findRepliesByParentId(@Param("parentId") Long parentId);

    @Select("SELECT COUNT(*) FROM discussions WHERE parent_id = #{discussionId}")
    Integer countRepliesByDiscussionId(@Param("discussionId") Long discussionId);

    @Update("UPDATE discussions SET title = #{title}, content = #{content}, updated_at = #{updatedAt} WHERE id = #{id}")
    void updateDiscussion(Discussion discussion);

    @Delete("DELETE FROM discussions WHERE id = #{id}")
    void deleteDiscussion(@Param("id") Long id);
}

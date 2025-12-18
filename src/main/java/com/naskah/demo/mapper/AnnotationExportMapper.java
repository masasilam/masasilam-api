package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.AnnotationExport;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AnnotationExportMapper {

    @Insert("INSERT INTO annotation_exports (" +
            "user_id, book_id, export_type, include_bookmarks, include_highlights, include_notes, " +
            "chapter_from, chapter_to, date_from, date_to, status, created_at, expires_at) " +
            "VALUES (#{userId}, #{bookId}, #{exportType}, #{includeBookmarks}, #{includeHighlights}, #{includeNotes}, " +
            "#{chapterFrom}, #{chapterTo}, #{dateFrom}, #{dateTo}, #{status}, #{createdAt}, #{expiresAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertExport(AnnotationExport export);

    @Update("UPDATE annotation_exports SET " +
            "status = #{status}, file_url = #{fileUrl}, file_size = #{fileSize}, file_name = #{fileName}, " +
            "total_bookmarks = #{totalBookmarks}, total_highlights = #{totalHighlights}, " +
            "total_notes = #{totalNotes}, error_message = #{errorMessage}, " +
            "started_at = #{startedAt}, completed_at = #{completedAt} " +
            "WHERE id = #{id}")
    void updateExport(AnnotationExport export);

    @Select("SELECT * FROM annotation_exports WHERE id = #{id}")
    AnnotationExport findById(@Param("id") Long id);

    @Select("SELECT * FROM annotation_exports " +
            "WHERE user_id = #{userId} " +
            "ORDER BY created_at DESC " +
            "LIMIT #{limit} OFFSET #{offset}")
    List<AnnotationExport> findUserExports(@Param("userId") Long userId,
                                           @Param("offset") int offset,
                                           @Param("limit") int limit);

    @Select("SELECT * FROM annotation_exports " +
            "WHERE status = 'PENDING' " +
            "ORDER BY created_at ASC " +
            "LIMIT #{limit}")
    List<AnnotationExport> findPendingExports(@Param("limit") int limit);

    @Delete("DELETE FROM annotation_exports " +
            "WHERE expires_at < NOW() AND status = 'COMPLETED'")
    int deleteExpiredExports();
}

package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.ProjectPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ProjectPageMapper {
    void insertProjectPage(ProjectPage projectPage);
    ProjectPage getProjectPageByProjectIdAndPageNumber(@Param("projectId") Long projectId, @Param("pageNumber") Integer pageNumber);
    void assignTranslatorToPage(@Param("projectId") Long projectId, @Param("pageNumber") Integer pageNumber, @Param("userId") Long userId);
    void assignEditorToPage(@Param("projectId") Long projectId, @Param("pageNumber") Integer pageNumber, @Param("userId") Long userId);
    void assignIllustratorToPage(@Param("projectId") Long projectId, @Param("pageNumber") Integer pageNumber, @Param("userId") Long userId);
    void assignProofreaderToPage(@Param("projectId") Long projectId, @Param("pageNumber") Integer pageNumber, @Param("userId") Long userId);
    void assignReviewerToPage(@Param("projectId") Long projectId, @Param("pageNumber") Integer pageNumber, @Param("userId") Long userId);
    void updateTranslationContent(@Param("projectId") Long projectId, @Param("pageNumber") Integer pageNumber, @Param("workContent") String workContent,
                                  @Param("userId") Long userId, @Param("notes") String notes);
    void updateEditingContent(@Param("projectId") Long projectId, @Param("pageNumber") Integer pageNumber, @Param("workContent") String workContent,
                              @Param("userId") Long userId, @Param("notes") String notes);
    void updateIllustrationContent(@Param("projectId") Long projectId, @Param("pageNumber") Integer pageNumber, @Param("workContent") String workContent,
                                   @Param("userId") Long userId, @Param("notes") String notes);
    void updateProofreadingContent(@Param("projectId") Long projectId, @Param("pageNumber") Integer pageNumber, @Param("workContent") String workContent,
                                   @Param("userId") Long userId, @Param("notes") String notes);
    void updateTranscriptionContent(@Param("projectId") Long projectId, @Param("pageNumber") Integer pageNumber, @Param("workContent") String workContent,
                                    @Param("userId") Long userId, @Param("notes") String notes);
    void updateReviewContent(@Param("projectId") Long projectId, @Param("pageNumber") Integer pageNumber, @Param("workContent") String workContent,
                             @Param("userId") Long userId, @Param("notes") String notes);
    void markTranslationCompleted(@Param("projectId") Long projectId, @Param("pageNumber") Integer pageNumber, @Param("userId") Long userId);
    void markEditingCompleted(@Param("projectId") Long projectId, @Param("pageNumber") Integer pageNumber, @Param("userId") Long userId);
    void markIllustrationCompleted(@Param("projectId") Long projectId, @Param("pageNumber") Integer pageNumber, @Param("userId") Long userId);
    void markProofreadingCompleted(@Param("projectId") Long projectId, @Param("pageNumber") Integer pageNumber, @Param("userId") Long userId);
    void markTranscriptionCompleted(@Param("projectId") Long projectId, @Param("pageNumber") Integer pageNumber, @Param("userId") Long userId);
    void markReviewCompleted(@Param("projectId") Long projectId, @Param("pageNumber") Integer pageNumber, @Param("userId") Long userId);
    void updatePageLastModified(@Param("projectId") Long projectId, @Param("pageNumber") Integer pageNumber, @Param("now") LocalDateTime now);
    int getCompletedTranslationPagesCount(@Param("projectId") Long projectId);
    int getCompletedEditingPagesCount(@Param("projectId") Long projectId);
    int getCompletedIllustrationPagesCount(@Param("projectId") Long projectId);
    int getCompletedProofreadingPagesCount(@Param("projectId") Long projectId);
    int getCompletedTranscriptionPagesCount(@Param("projectId") Long projectId);

//    List<ProjectPageExportData> getProjectPagesForExport(Long id);

    void updatePageReactionCount(@Param("pageId") Long pageId, @Param("reactionCount") Long reactionCount);
    void updatePageCommentCount(@Param("pageId") Long pageId, @Param("commentCount") Long commentCount);
    Integer getUserEditingCount(@Param("userId") Long userId);
    Integer getUserTranslationCount(@Param("userId") Long userId);
    Integer getUserProofreadingCount(@Param("userId") Long userId);
    Integer getUserIllustrationCount(@Param("userId") Long userId);
    Integer getUserTranscriptionCount(@Param("userId") Long userId);
    Integer getUserReviewCount(@Param("userId") Long userId);
}
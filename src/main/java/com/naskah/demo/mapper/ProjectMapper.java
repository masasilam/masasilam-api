package com.naskah.demo.mapper;

import com.naskah.demo.model.dto.response.ProjectActivityResponse;
import com.naskah.demo.model.dto.response.ProjectResponse;
import com.naskah.demo.model.dto.response.UserActivityResponse;
import com.naskah.demo.model.entity.Project;
import com.naskah.demo.model.entity.ProjectActivity;
import com.naskah.demo.model.entity.ProjectQualityAssessment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ProjectMapper {
    List<ProjectResponse> getProjectListWithFilters(@Param("status") String status, @Param("difficulty") String difficulty,
                                                    @Param("title") String title, @Param("genre") String genre, @Param("offset") int offset,
                                                    @Param("limit") int limit, @Param("sortColumn") String sortColumn, @Param("sortType") String sortType);
    void insertProject(Project project);
    ProjectResponse getProjectById(@Param("id") Long id);
    int updateProject(Project project);
    Project getProjectBySlug(@Param("slug") String slug);
    void updateMemberCount(@Param("projectId") Long projectId, @Param("memberCount") int memberCount);
    void updateProjectTimestamp(@Param("projectId") Long projectId, @Param("now") LocalDateTime now);
    void updateTranslationCompletedPages(@Param("projectId") Long projectId, @Param("completedPages") int completedPages);
    void updateEditingCompletedPages(@Param("projectId") Long projectId, @Param("completedPages") int completedPages);
    void updateIllustrationCompletedPages(@Param("projectId") Long projectId, @Param("completedPages") int completedPages);
    void updateProofreadingCompletedPages(@Param("projectId") Long projectId, @Param("completedPages") int completedPages);
    void updateTranscriptionCompletedPages(@Param("projectId") Long projectId, @Param("completedPages") int completedPages);
    Project getProjectByIdEntity(@Param("projectId") Long projectId);
    void updateOverallProgress(@Param("projectId") Long projectId, @Param("overallProgress") BigDecimal overallProgress);
    void incrementViewCount(@Param("id") Long id);
    List<ProjectResponse> getProjectsByCreator(@Param("userId") Long userId);
    void insertQualityAssessment(ProjectQualityAssessment assessment);
    void updateReactionCount(@Param("projectId") Long projectId, @Param("reactionCount") Long reactionCount);
    void updateCommentCount(@Param("projectId") Long projectId, @Param("commentCount") Long commentCount);
    void updateFollowerCount(@Param("projectId") Long projectId, @Param("followerCount") Long followerCount);
    List<ProjectActivityResponse> getRecentProjectActivity(@Param("projectId") Long projectId, @Param("limit") int limit);
    Integer getUserCreatedProjectCount(@Param("userId") Long userId);
    BigDecimal getUserAverageQualityScore(@Param("userId") Long userId);
    List<UserActivityResponse> getUserRecentActivity(@Param("userId") Long userId, @Param("limit") int limit);
    BigDecimal getProjectAverageQualityScore(@Param("projectId") Long projectId);
    void updateProjectQualityScore(@Param("projectId") Long projectId, @Param("averageScore") BigDecimal averageScore);
    void insertProjectActivity(ProjectActivity activity);
}
package com.masasilam.app.mapper;

import com.masasilam.app.model.dto.response.ProjectResponse;
import com.masasilam.app.model.entity.ProjectFollow;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ProjectFollowMapper {
    ProjectFollow getProjectFollowByUserAndProject(Long userId, Long id);

    void reactivateProjectFollow(Long id);

    void deactivateProjectFollow(Long id);

    Long getActiveFollowerCountByProjectId(Long id);

    List<ProjectResponse> getUserFollowedProjects(Long userId);

    void insertProjectFollow(ProjectFollow follow);

    List<Long> getActiveFollowerIdsByProjectId(Long projectId);

    Integer getUserFollowedProjectCount(Long userId);
}

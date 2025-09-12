package com.naskah.demo.mapper;

import com.naskah.demo.model.dto.response.ProjectResponse;
import com.naskah.demo.model.entity.ProjectFollow;
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

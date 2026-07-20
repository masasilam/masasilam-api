package com.masasilam.app.mapper;

import com.masasilam.app.model.dto.response.MemberRoleSummary;
import com.masasilam.app.model.dto.response.ProjectMembershipResponse;
import com.masasilam.app.model.entity.ProjectMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProjectMemberMapper {
    ProjectMember getProjectMemberByProjectIdAndUserId(@Param("projectId") Long projectId, @Param("userId") Long userId);

    List<ProjectMember> getProjectMembersByProjectIdAndRole(@Param("projectId") Long projectId, @Param("role") String role);

    void insertProjectMember(ProjectMember projectMember);

    int getActiveMemberCountByProjectId(@Param("projectId") Long projectId);

    List<MemberRoleSummary> getProjectMemberRoleBreakdown(@Param("id") Long id);

    List<ProjectMembershipResponse> getUserProjectMemberships(@Param("userId") Long userId);

    List<Long> getActiveMemberIdsByProjectId(@Param("projectId") Long projectId);

    Integer getUserJoinedProjectCount(@Param("userId") Long userId);
}

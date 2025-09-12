package com.naskah.demo.service;

import com.naskah.demo.model.dto.request.*;
import com.naskah.demo.model.dto.response.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProjectService {
    DatatableResponse<ProjectResponse> getProjects(int page, int limit, String sortField, String sortOrder,
                                                   String status, String difficulty, String title, String genre);

    DataResponse<ProjectResponse> createProject(CreateProjectRequest request, List<MultipartFile> files);

    DataResponse<String> joinProject(String projectSlug, JoinProjectRequest request);

    DataResponse<String> submitWork(String projectSlug, Integer page, SubmitWorkRequest request);

    DataResponse<ProjectReactionResponse> reactToProject(String projectSlug, ProjectReactionRequest request);

    DataResponse<ProjectCommentResponse> addProjectComment(String projectSlug, ProjectCommentRequest request);

    DataResponse<PageReactionResponse> reactToPage(String projectSlug, Integer pageNumber, PageReactionRequest request);

    DataResponse<PageCommentResponse> addPageComment(String projectSlug, Integer pageNumber, PageCommentRequest request);

    DatatableResponse<ProjectCommentResponse> getProjectComments(String projectSlug, int page, int limit, String sortOrder);

    DatatableResponse<PageCommentResponse> getPageComments(String projectSlug, Integer pageNumber, int page, int limit, String sortOrder);

    DataResponse<String> followProject(String projectSlug);

    DataResponse<String> unfollowProject(String projectSlug);

    DataResponse<ProjectStatisticsResponse> getProjectStatistics(String projectSlug);

    DataResponse<String> assessProjectQuality(String projectSlug, ProjectQualityAssessmentRequest request);

    DataResponse<String> markProjectComplete(String projectSlug);
//    DataResponse<ProjectExportResponse> exportProject(String projectSlug, String format);
//    DataResponse<ProjectPageDetailResponse> getProjectPageDetail(String projectSlug, Integer pageNumber);
//    DatatableResponse<NotificationResponse> getUserNotifications(int page, int limit, String status);
//    DataResponse<String> markNotificationAsRead(Long notificationId);
//    DatatableResponse<ProjectMemberResponse> getProjectMembers(String projectSlug, int page, int limit, String role);
//    DataResponse<String> removeMemberFromProject(String projectSlug, Long memberId);
//    DataResponse<String> updateMemberAssignment(String projectSlug, Long memberId, UpdateMemberAssignmentRequest request);
//    DataResponse<String> reportPageIssue(String projectSlug, Integer pageNumber, ReportIssueRequest request);
//    DataResponse<String> resolvePageIssue(String projectSlug, Long issueId, ResolveIssueRequest request);
//    DatatableResponse<ProjectIssueResponse> getProjectIssues(String projectSlug, int page, int limit, String status);
}
package com.naskah.demo.controller;

import com.naskah.demo.model.dto.request.*;
import com.naskah.demo.model.dto.response.*;
import com.naskah.demo.service.ProjectService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public ResponseEntity<DatatableResponse<ProjectResponse>> getProjects(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) int limit,
            @RequestParam(defaultValue = "updatedAt", required = false) String sortField,
            @RequestParam(defaultValue = "DESC", required = false) String sortOrder,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String genre) {
        DatatableResponse<ProjectResponse> response = projectService.getProjects(
                page, limit, sortField, sortOrder, status, difficulty, title, genre);
        return ResponseEntity.ok(response);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DataResponse<ProjectResponse>> createProject(
            @RequestPart("project") @Valid CreateProjectRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        DataResponse<ProjectResponse> response = projectService.createProject(request, files);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{projectSlug}/join")
    public ResponseEntity<DataResponse<String>> joinProject(
            @PathVariable String projectSlug,
            @RequestBody @Valid JoinProjectRequest request) {
        DataResponse<String> response = projectService.joinProject(projectSlug, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{projectSlug}/{pageNumber}/submit")
    public ResponseEntity<DataResponse<String>> submitWork(
            @PathVariable String projectSlug,
            @PathVariable Integer pageNumber,
            @RequestBody @Valid SubmitWorkRequest request) {
        DataResponse<String> response = projectService.submitWork(projectSlug, pageNumber, request);
        return ResponseEntity.ok(response);
    }

    // NEW ENDPOINTS: Reaction System

    @PostMapping("/{projectSlug}/react")
    public ResponseEntity<DataResponse<ProjectReactionResponse>> reactToProject(
            @PathVariable String projectSlug,
            @RequestBody @Valid ProjectReactionRequest request) {
        DataResponse<ProjectReactionResponse> response = projectService.reactToProject(projectSlug, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{projectSlug}/comment")
    public ResponseEntity<DataResponse<ProjectCommentResponse>> addProjectComment(
            @PathVariable String projectSlug,
            @RequestBody @Valid ProjectCommentRequest request) {
        DataResponse<ProjectCommentResponse> response = projectService.addProjectComment(projectSlug, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{projectSlug}/{pageNumber}/react")
    public ResponseEntity<DataResponse<PageReactionResponse>> reactToPage(
            @PathVariable String projectSlug,
            @PathVariable Integer pageNumber,
            @RequestBody @Valid PageReactionRequest request) {
        DataResponse<PageReactionResponse> response = projectService.reactToPage(projectSlug, pageNumber, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{projectSlug}/{pageNumber}/comment")
    public ResponseEntity<DataResponse<PageCommentResponse>> addPageComment(
            @PathVariable String projectSlug,
            @PathVariable Integer pageNumber,
            @RequestBody @Valid PageCommentRequest request) {
        DataResponse<PageCommentResponse> response = projectService.addPageComment(projectSlug, pageNumber, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{projectSlug}/comments")
    public ResponseEntity<DatatableResponse<ProjectCommentResponse>> getProjectComments(
            @PathVariable String projectSlug,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) int limit,
            @RequestParam(defaultValue = "DESC", required = false) String sortOrder) {
        DatatableResponse<ProjectCommentResponse> response = projectService.getProjectComments(
                projectSlug, page, limit, sortOrder);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{projectSlug}/{pageNumber}/comments")
    public ResponseEntity<DatatableResponse<PageCommentResponse>> getPageComments(
            @PathVariable String projectSlug,
            @PathVariable Integer pageNumber,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) int limit,
            @RequestParam(defaultValue = "DESC", required = false) String sortOrder) {
        DatatableResponse<PageCommentResponse> response = projectService.getPageComments(
                projectSlug, pageNumber, page, limit, sortOrder);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{projectSlug}/follow")
    public ResponseEntity<DataResponse<String>> followProject(@PathVariable String projectSlug) {
        DataResponse<String> response = projectService.followProject(projectSlug);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{projectSlug}/follow")
    public ResponseEntity<DataResponse<String>> unfollowProject(@PathVariable String projectSlug) {
        DataResponse<String> response = projectService.unfollowProject(projectSlug);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{projectSlug}/statistics")
    public ResponseEntity<DataResponse<ProjectStatisticsResponse>> getProjectStatistics(
            @PathVariable String projectSlug) {
        DataResponse<ProjectStatisticsResponse> response = projectService.getProjectStatistics(projectSlug);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{projectSlug}/assess-quality")
    public ResponseEntity<DataResponse<String>> assessProjectQuality(
            @PathVariable String projectSlug,
            @RequestBody @Valid ProjectQualityAssessmentRequest request) {
        DataResponse<String> response = projectService.assessProjectQuality(projectSlug, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{projectSlug}/complete")
    public ResponseEntity<DataResponse<String>> markProjectComplete(@PathVariable String projectSlug) {
        DataResponse<String> response = projectService.markProjectComplete(projectSlug);
        return ResponseEntity.ok(response);
    }

//    @GetMapping("/{projectSlug}/export")
//    public ResponseEntity<DataResponse<ProjectExportResponse>> exportProject(
//            @PathVariable String projectSlug,
//            @RequestParam(defaultValue = "PDF") String format) {
//        DataResponse<ProjectExportResponse> response = projectService.exportProject(projectSlug, format);
//        return ResponseEntity.ok(response);
//    }

//    // NEW ENDPOINTS: Page Management
//
//    @GetMapping("/{projectSlug}/{pageNumber}")
//    public ResponseEntity<DataResponse<ProjectPageDetailResponse>> getProjectPageDetail(
//            @PathVariable String projectSlug,
//            @PathVariable Integer pageNumber) {
//        DataResponse<ProjectPageDetailResponse> response = projectService.getProjectPageDetail(projectSlug, pageNumber);
//        return ResponseEntity.ok(response);
//    }
//
//    // NEW ENDPOINTS: Member Management
//
//    @GetMapping("/{projectSlug}/members")
//    public ResponseEntity<DatatableResponse<ProjectMemberResponse>> getProjectMembers(
//            @PathVariable String projectSlug,
//            @RequestParam(defaultValue = "1") @Min(1) int page,
//            @RequestParam(defaultValue = "10") @Min(1) int limit,
//            @RequestParam(required = false) String role) {
//        DatatableResponse<ProjectMemberResponse> response = projectService.getProjectMembers(
//                projectSlug, page, limit, role);
//        return ResponseEntity.ok(response);
//    }
//
//    @DeleteMapping("/{projectSlug}/members/{memberId}")
//    public ResponseEntity<DataResponse<String>> removeMemberFromProject(
//            @PathVariable String projectSlug,
//            @PathVariable Long memberId) {
//        DataResponse<String> response = projectService.removeMemberFromProject(projectSlug, memberId);
//        return ResponseEntity.ok(response);
//    }
//
//    @PutMapping("/{projectSlug}/members/{memberId}")
//    public ResponseEntity<DataResponse<String>> updateMemberAssignment(
//            @PathVariable String projectSlug,
//            @PathVariable Long memberId,
//            @RequestBody @Valid UpdateMemberAssignmentRequest request) {
//        DataResponse<String> response = projectService.updateMemberAssignment(projectSlug, memberId, request);
//        return ResponseEntity.ok(response);
//    }
//
//    // NEW ENDPOINTS: Issue Management
//
//    @PostMapping("/{projectSlug}/{pageNumber}/report-issue")
//    public ResponseEntity<DataResponse<String>> reportPageIssue(
//            @PathVariable String projectSlug,
//            @PathVariable Integer pageNumber,
//            @RequestBody @Valid ReportIssueRequest request) {
//        DataResponse<String> response = projectService.reportPageIssue(projectSlug, pageNumber, request);
//        return ResponseEntity.ok(response);
//    }
//
//    @PutMapping("/{projectSlug}/issues/{issueId}/resolve")
//    public ResponseEntity<DataResponse<String>> resolvePageIssue(
//            @PathVariable String projectSlug,
//            @PathVariable Long issueId,
//            @RequestBody @Valid ResolveIssueRequest request) {
//        DataResponse<String> response = projectService.resolvePageIssue(projectSlug, issueId, request);
//        return ResponseEntity.ok(response);
//    }
//
//    @GetMapping("/{projectSlug}/issues")
//    public ResponseEntity<DatatableResponse<ProjectIssueResponse>> getProjectIssues(
//            @PathVariable String projectSlug,
//            @RequestParam(defaultValue = "1") @Min(1) int page,
//            @RequestParam(defaultValue = "10") @Min(1) int limit,
//            @RequestParam(required = false) String status) {
//        DatatableResponse<ProjectIssueResponse> response = projectService.getProjectIssues(
//                projectSlug, page, limit, status);
//        return ResponseEntity.ok(response);
//    }
//
//    // NEW ENDPOINTS: Notification Management
//
//    @GetMapping("/notifications")
//    public ResponseEntity<DatatableResponse<NotificationResponse>> getUserNotifications(
//            @RequestParam(defaultValue = "1") @Min(1) int page,
//            @RequestParam(defaultValue = "20") @Min(1) int limit,
//            @RequestParam(required = false) String status) {
//        DatatableResponse<NotificationResponse> response = projectService.getUserNotifications(page, limit, status);
//        return ResponseEntity.ok(response);
//    }
//
//    @PutMapping("/notifications/{notificationId}/read")
//    public ResponseEntity<DataResponse<String>> markNotificationAsRead(@PathVariable Long notificationId) {
//        DataResponse<String> response = projectService.markNotificationAsRead(notificationId);
//        return ResponseEntity.ok(response);
//    }
//
//    // NEW ENDPOINTS: Advanced Search and Analytics
//
//    @GetMapping("/search")
//    public ResponseEntity<DatatableResponse<ProjectResponse>> searchProjects(
//            @RequestParam String query,
//            @RequestParam(defaultValue = "1") @Min(1) int page,
//            @RequestParam(defaultValue = "10") @Min(1) int limit,
//            @RequestParam(defaultValue = "relevance") String sortBy) {
//        DatatableResponse<ProjectResponse> response = projectService.searchProjects(query, page, limit, sortBy);
//        return ResponseEntity.ok(response);
//    }
//
//    @GetMapping("/trending")
//    public ResponseEntity<DataResponse<List<ProjectResponse>>> getTrendingProjects(
//            @RequestParam(defaultValue = "7") int days,
//            @RequestParam(defaultValue = "10") int limit) {
//        DataResponse<List<ProjectResponse>> response = projectService.getTrendingProjects(days, limit);
//        return ResponseEntity.ok(response);
//    }
//
//    @GetMapping("/featured")
//    public ResponseEntity<DataResponse<List<ProjectResponse>>> getFeaturedProjects(
//            @RequestParam(defaultValue = "5") int limit) {
//        DataResponse<List<ProjectResponse>> response = projectService.getFeaturedProjects(limit);
//        return ResponseEntity.ok(response);
//    }
//
//    // NEW ENDPOINTS: Collaboration Features
//
//    @PostMapping("/{projectSlug}/collaborate-invite")
//    public ResponseEntity<DataResponse<String>> inviteCollaborator(
//            @PathVariable String projectSlug,
//            @RequestBody @Valid CollaboratorInviteRequest request) {
//        DataResponse<String> response = projectService.inviteCollaborator(projectSlug, request);
//        return ResponseEntity.ok(response);
//    }
//
//    @PostMapping("/{projectSlug}/collaborate-accept/{inviteToken}")
//    public ResponseEntity<DataResponse<String>> acceptCollaborationInvite(
//            @PathVariable String projectSlug,
//            @PathVariable String inviteToken) {
//        DataResponse<String> response = projectService.acceptCollaborationInvite(projectSlug, inviteToken);
//        return ResponseEntity.ok(response);
//    }
//
//    // NEW ENDPOINTS: Version Control and History
//
//    @GetMapping("/{projectSlug}/{pageNumber}/history")
//    public ResponseEntity<DatatableResponse<PageVersionHistoryResponse>> getPageVersionHistory(
//            @PathVariable String projectSlug,
//            @PathVariable Integer pageNumber,
//            @RequestParam(defaultValue = "1") @Min(1) int page,
//            @RequestParam(defaultValue = "10") @Min(1) int limit) {
//        DatatableResponse<PageVersionHistoryResponse> response = projectService.getPageVersionHistory(
//                projectSlug, pageNumber, page, limit);
//        return ResponseEntity.ok(response);
//    }
//
//    @PostMapping("/{projectSlug}/{pageNumber}/revert/{versionId}")
//    public ResponseEntity<DataResponse<String>> revertPageToVersion(
//            @PathVariable String projectSlug,
//            @PathVariable Integer pageNumber,
//            @PathVariable Long versionId) {
//        DataResponse<String> response = projectService.revertPageToVersion(projectSlug, pageNumber, versionId);
//        return ResponseEntity.ok(response);
//    }
//
//    // NEW ENDPOINTS: Real-time Collaboration
//
//    @PostMapping("/{projectSlug}/{pageNumber}/lock")
//    public ResponseEntity<DataResponse<String>> lockPageForEditing(
//            @PathVariable String projectSlug,
//            @PathVariable Integer pageNumber) {
//        DataResponse<String> response = projectService.lockPageForEditing(projectSlug, pageNumber);
//        return ResponseEntity.ok(response);
//    }
//
//    @DeleteMapping("/{projectSlug}/{pageNumber}/lock")
//    public ResponseEntity<DataResponse<String>> unlockPageFromEditing(
//            @PathVariable String projectSlug,
//            @PathVariable Integer pageNumber) {
//        DataResponse<String> response = projectService.unlockPageFromEditing(projectSlug, pageNumber);
//        return ResponseEntity.ok(response);
//    }
//
//    // NEW ENDPOINTS: Templates and Presets
//
//    @GetMapping("/templates")
//    public ResponseEntity<DataResponse<List<ProjectTemplateResponse>>> getProjectTemplates() {
//        DataResponse<List<ProjectTemplateResponse>> response = projectService.getProjectTemplates();
//        return ResponseEntity.ok(response);
//    }
//
//    @PostMapping("/from-template/{templateId}")
//    public ResponseEntity<DataResponse<ProjectResponse>> createProjectFromTemplate(
//            @PathVariable Long templateId,
//            @RequestBody @Valid CreateProjectFromTemplateRequest request) {
//        DataResponse<ProjectResponse> response = projectService.createProjectFromTemplate(templateId, request);
//        return ResponseEntity.ok(response);
//    }
//
//    // NEW ENDPOINTS: Batch Operations
//
//    @PostMapping("/{projectSlug}/batch-assign")
//    public ResponseEntity<DataResponse<String>> batchAssignPages(
//            @PathVariable String projectSlug,
//            @RequestBody @Valid BatchPageAssignmentRequest request) {
//        DataResponse<String> response = projectService.batchAssignPages(projectSlug, request);
//        return ResponseEntity.ok(response);
//    }
//
//    @PostMapping("/{projectSlug}/batch-update-status")
//    public ResponseEntity<DataResponse<String>> batchUpdatePageStatus(
//            @PathVariable String projectSlug,
//            @RequestBody @Valid BatchPageStatusUpdateRequest request) {
//        DataResponse<String> response = projectService.batchUpdatePageStatus(projectSlug, request);
//        return ResponseEntity.ok(response);
//    }
//
//    // NEW ENDPOINTS: Integration and API
//
//    @PostMapping("/{projectSlug}/webhook")
//    public ResponseEntity<DataResponse<String>> configureWebhook(
//            @PathVariable String projectSlug,
//            @RequestBody @Valid WebhookConfigRequest request) {
//        DataResponse<String> response = projectService.configureWebhook(projectSlug, request);
//        return ResponseEntity.ok(response);
//    }
//
//    @GetMapping("/{projectSlug}/api-key")
//    public ResponseEntity<DataResponse<ProjectApiKeyResponse>> generateApiKey(@PathVariable String projectSlug) {
//        DataResponse<ProjectApiKeyResponse> response = projectService.generateProjectApiKey(projectSlug);
//        return ResponseEntity.ok(response);
//    }
}
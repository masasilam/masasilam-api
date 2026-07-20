package com.masasilam.app.controller;

import com.masasilam.app.model.dto.request.*;
import com.masasilam.app.model.dto.response.*;
import com.masasilam.app.service.common.ProjectService;
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
    public ResponseEntity<DatatableResponse<ProjectResponse>> getProjects(@RequestParam(defaultValue = "1") @Min(1) int page,
                                                                          @RequestParam(defaultValue = "10") @Min(1) int limit,
                                                                          @RequestParam(defaultValue = "updatedAt", required = false) String sortField,
                                                                          @RequestParam(defaultValue = "DESC", required = false) String sortOrder,
                                                                          @RequestParam(required = false) String status,
                                                                          @RequestParam(required = false) String difficulty,
                                                                          @RequestParam(required = false) String title,
                                                                          @RequestParam(required = false) String genre) {
        DatatableResponse<ProjectResponse> response = projectService.getProjects(page, limit, sortField, sortOrder, status, difficulty, title, genre);
        return ResponseEntity.ok(response);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DataResponse<ProjectResponse>> createProject(@RequestPart("project") @Valid CreateProjectRequest request,
                                                                       @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        DataResponse<ProjectResponse> response = projectService.createProject(request, files);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{projectSlug}/join")
    public ResponseEntity<DataResponse<String>> joinProject(@PathVariable String projectSlug,
                                                            @RequestBody @Valid JoinProjectRequest request) {
        DataResponse<String> response = projectService.joinProject(projectSlug, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{projectSlug}/{pageNumber}/submit")
    public ResponseEntity<DataResponse<String>> submitWork(@PathVariable String projectSlug,
                                                           @PathVariable Integer pageNumber,
                                                           @RequestBody @Valid SubmitWorkRequest request) {
        DataResponse<String> response = projectService.submitWork(projectSlug, pageNumber, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{projectSlug}/react")
    public ResponseEntity<DataResponse<ProjectReactionResponse>> reactToProject(@PathVariable String projectSlug,
                                                                                @RequestBody @Valid ProjectReactionRequest request) {
        DataResponse<ProjectReactionResponse> response = projectService.reactToProject(projectSlug, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{projectSlug}/comment")
    public ResponseEntity<DataResponse<ProjectCommentResponse>> addProjectComment(@PathVariable String projectSlug,
                                                                                  @RequestBody @Valid ProjectCommentRequest request) {
        DataResponse<ProjectCommentResponse> response = projectService.addProjectComment(projectSlug, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{projectSlug}/{pageNumber}/react")
    public ResponseEntity<DataResponse<PageReactionResponse>> reactToPage(@PathVariable String projectSlug,
                                                                          @PathVariable Integer pageNumber,
                                                                          @RequestBody @Valid PageReactionRequest request) {
        DataResponse<PageReactionResponse> response = projectService.reactToPage(projectSlug, pageNumber, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{projectSlug}/{pageNumber}/comment")
    public ResponseEntity<DataResponse<PageCommentResponse>> addPageComment(@PathVariable String projectSlug,
                                                                            @PathVariable Integer pageNumber,
                                                                            @RequestBody @Valid PageCommentRequest request) {
        DataResponse<PageCommentResponse> response = projectService.addPageComment(projectSlug, pageNumber, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{projectSlug}/comments")
    public ResponseEntity<DatatableResponse<ProjectCommentResponse>> getProjectComments(@PathVariable String projectSlug,
                                                                                        @RequestParam(defaultValue = "1") @Min(1) int page,
                                                                                        @RequestParam(defaultValue = "10") @Min(1) int limit,
                                                                                        @RequestParam(defaultValue = "DESC", required = false) String sortOrder) {
        DatatableResponse<ProjectCommentResponse> response = projectService.getProjectComments(projectSlug, page, limit, sortOrder);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{projectSlug}/{pageNumber}/comments")
    public ResponseEntity<DatatableResponse<PageCommentResponse>> getPageComments(@PathVariable String projectSlug,
                                                                                  @PathVariable Integer pageNumber,
                                                                                  @RequestParam(defaultValue = "1") @Min(1) int page,
                                                                                  @RequestParam(defaultValue = "10") @Min(1) int limit,
                                                                                  @RequestParam(defaultValue = "DESC", required = false) String sortOrder) {
        DatatableResponse<PageCommentResponse> response = projectService.getPageComments(projectSlug, pageNumber, page, limit, sortOrder);
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
    public ResponseEntity<DataResponse<ProjectStatisticsResponse>> getProjectStatistics(@PathVariable String projectSlug) {
        DataResponse<ProjectStatisticsResponse> response = projectService.getProjectStatistics(projectSlug);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{projectSlug}/assess-quality")
    public ResponseEntity<DataResponse<String>> assessProjectQuality(@PathVariable String projectSlug,
                                                                     @RequestBody @Valid ProjectQualityAssessmentRequest request) {
        DataResponse<String> response = projectService.assessProjectQuality(projectSlug, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{projectSlug}/complete")
    public ResponseEntity<DataResponse<String>> markProjectComplete(@PathVariable String projectSlug) {
        DataResponse<String> response = projectService.markProjectComplete(projectSlug);
        return ResponseEntity.ok(response);
    }
}
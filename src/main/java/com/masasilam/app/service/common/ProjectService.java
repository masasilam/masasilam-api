package com.masasilam.app.service.common;

import com.masasilam.app.model.dto.request.CreateProjectRequest;
import com.masasilam.app.model.dto.request.JoinProjectRequest;
import com.masasilam.app.model.dto.request.PageCommentRequest;
import com.masasilam.app.model.dto.request.PageReactionRequest;
import com.masasilam.app.model.dto.request.ProjectCommentRequest;
import com.masasilam.app.model.dto.request.ProjectQualityAssessmentRequest;
import com.masasilam.app.model.dto.request.ProjectReactionRequest;
import com.masasilam.app.model.dto.request.SubmitWorkRequest;
import com.masasilam.app.model.dto.response.DataResponse;
import com.masasilam.app.model.dto.response.DatatableResponse;
import com.masasilam.app.model.dto.response.PageCommentResponse;
import com.masasilam.app.model.dto.response.PageReactionResponse;
import com.masasilam.app.model.dto.response.ProjectCommentResponse;
import com.masasilam.app.model.dto.response.ProjectReactionResponse;
import com.masasilam.app.model.dto.response.ProjectResponse;
import com.masasilam.app.model.dto.response.ProjectStatisticsResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProjectService {
    DatatableResponse<ProjectResponse> getProjects(int page, int limit, String sortField, String sortOrder, String status, String difficulty, String title, String genre);
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
}
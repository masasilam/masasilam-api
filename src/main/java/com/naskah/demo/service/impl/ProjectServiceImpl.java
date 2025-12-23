package com.naskah.demo.service.impl;

import com.naskah.demo.exception.custom.DataNotFoundException;
import com.naskah.demo.mapper.*;
import com.naskah.demo.model.dto.request.*;
import com.naskah.demo.model.entity.*;
import com.naskah.demo.model.enums.*;
import com.naskah.demo.model.dto.response.*;
import com.naskah.demo.service.ProjectService;
import com.naskah.demo.util.file.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectServiceImpl implements ProjectService {

    private final ProjectMapper projectMapper;
    private final ProjectPageMapper projectPageMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final ProjectReactionMapper projectReactionMapper;
    private final ProjectCommentMapper projectCommentMapper;
    private final PageReactionMapper pageReactionMapper;
    private final PageCommentMapper pageCommentMapper;
    private final NotificationMapper notificationMapper;
    private final ProjectFollowMapper projectFollowMapper;
    private final FileUtil fileUtil;

    private static final String SUCCESS = "Success";

    @Value("${file.upload.directory:uploads}")
    private String uploadDirectory;

    // getProjects
    @Override
    public DatatableResponse<ProjectResponse> getProjects(int page, int limit, String sortField, String sortOrder,
                                                          String status, String difficulty, String title, String genre) {
        Map<String, String> allowedSortFields = new HashMap<>();
        allowedSortFields.put("updateAt", "UPDATE_AT");
        allowedSortFields.put("title", "TITLE");
        allowedSortFields.put("publishedAt", "PUBLISHED_AT");
        allowedSortFields.put("author", "AUTHOR");
        allowedSortFields.put("expectedCompletionDate", "EXPECTED_COMPLETION_DATE");
        allowedSortFields.put("overallProgress", "OVERALL_PROGRESS");
        allowedSortFields.put("qualityScore", "QUALITY_SCORE");
        allowedSortFields.put("viewCount", "VIEW_COUNT");
        allowedSortFields.put("followerCount", "FOLLOWER_COUNT");
        allowedSortFields.put("reactionCount", "REACTION_COUNT"); // New sort field

        String sortColumn = allowedSortFields.getOrDefault(sortField, "UPDATE_AT");
        String sortType = Objects.equals(sortOrder, "DESC") ? "DESC" : "ASC";
        int offset = (page - 1) * limit;

        List<ProjectResponse> pageResult = projectMapper.getProjectListWithFilters(
                status, difficulty, title, genre, offset, limit, sortColumn, sortType);

        Long currentUserId = getCurrentUserId();
        for (ProjectResponse project : pageResult) {
            enhanceProjectWithEngagementData(project, currentUserId);
        }

        PageDataResponse<ProjectResponse> data = new PageDataResponse<>(page, limit, pageResult.size(), pageResult);
        return new DatatableResponse<>(SUCCESS, ResponseMessage.DATA_FETCHED, HttpStatus.OK.value(), data);
    }

    // createProject method
    @Override
    @Transactional
    public DataResponse<ProjectResponse> createProject(CreateProjectRequest request, List<MultipartFile> files) {
        try {
            Long userId = getCurrentUserId();

            // Create project entity
            Project project = new Project();
            project.setTitle(request.getTitle());
            project.setSlug(fileUtil.sanitizeFilename(request.getTitle()));
            project.setDescription(request.getDescription());
            project.setAuthor(request.getAuthor());
            project.setOriginalSource(request.getOriginalSource());
            project.setDifficulty(DifficultyLevel.valueOf(request.getDifficulty()));
            project.setStatus(ProjectStatus.PLANNING);
            project.setPriority(ProjectPriority.valueOf(request.getPriority()));
            project.setOriginalLanguage(request.getLanguage());
            project.setOriginalTitle(request.getTitle());
            project.setIsPublicDomain(true);
            project.setCopyrightStatus("PUBLIC DOMAIN");
            project.setEstimatedPages(request.getEstimatedPages());
            project.setEstimatedWordCount(0);
            project.setCreatedBy(userId);
            project.setCreatedAt(LocalDateTime.now());
            project.setUpdatedAt(LocalDateTime.now());
            project.setTotalPages(0);
            project.setTranscriptionCompletedPages(0);
            project.setTranslationCompletedPages(0);
            project.setEditingCompletedPages(0);
            project.setIllustrationCompletedPages(0);
            project.setFormattedPages(0);
            project.setPagesCompleted(0);
            project.setOverallProgress(BigDecimal.ZERO);
            project.setIssuesReported(0);
            project.setIssuesResolved(0);
            project.setIsPublished(false);
            project.setViewCount(0L);
            project.setFollowerCount(0L);
            project.setReactionCount(0L);
            project.setCommentCount(0L);

            // Insert project to get ID
            projectMapper.insertProject(project);

            // Auto-follow for project creator
            followProject(project.getId(), userId);

            log.info("Created project with ID: {}", project.getId());

            int totalPagesCreated = 0;

            // Process uploaded files (keeping existing logic)
            if (files != null && !files.isEmpty()) {
                log.info("Processing {} uploaded files for project {}", files.size(), project.getId());

                List<MultipartFile> sortedFiles = new ArrayList<>(files);
                sortedFiles.sort(Comparator.comparing(MultipartFile::getOriginalFilename,
                        Comparator.nullsLast(String::compareToIgnoreCase)));

                for (MultipartFile file : sortedFiles) {
                    try {
                        long maxFileSize = fileUtil.parseFileSize("50MB");
                        fileUtil.validateFile(file, maxFileSize);
                        Path filePath = fileUtil.saveFile(file, uploadDirectory, project.getId());
                        String extension = fileUtil.getFileExtension(file.getOriginalFilename());
                        FileType fileType = fileUtil.determineFileType(extension);

                        List<ProjectPage> pages = new ArrayList<>();
                        int pagesCreated;

                        switch (fileType) {
                            case PDF:
                                try {
                                    List<String> extractedContent = fileUtil.extractTextFromPDF(filePath);
                                    if (extractedContent.isEmpty() || extractedContent.stream().allMatch(String::isEmpty)) {
                                        log.info("PDF contains images, will require OCR processing");
                                        pages = fileUtil.createPDFPagesForOCR(project.getId(), filePath,
                                                file.getOriginalFilename());
                                    } else {
                                        pages = fileUtil.createPDFPagesWithText(project.getId(), filePath,
                                                file.getOriginalFilename(), extractedContent);
                                    }
                                    pagesCreated = pages.size();
                                } catch (Exception e) {
                                    log.error("Failed to process PDF file {}: {}", file.getOriginalFilename(), e.getMessage());
                                    throw e;
                                }
                                break;

                            case EPUB:
                                try {
//                                    List<String> extractedContent = fileUtil.extractTextFromEPUB(filePath);
//                                    pages = fileUtil.createEPUBPages(project.getId(),
//                                            file.getOriginalFilename(), extractedContent);
                                    pagesCreated = 0;
                                } catch (Exception e) {
                                    log.error("Failed to process EPUB file {}: {}", file.getOriginalFilename(), e.getMessage());
                                    throw e;
                                }
                                break;

                            case WORD:
                                try {
                                    List<String> extractedContent = fileUtil.extractTextFromWord(filePath);
                                    pages = fileUtil.createWordPages(project.getId(),
                                            file.getOriginalFilename(), extractedContent);
                                    pagesCreated = pages.size();
                                } catch (Exception e) {
                                    log.error("Failed to process Word file {}: {}", file.getOriginalFilename(), e.getMessage());
                                    throw e;
                                }
                                break;

                            case IMAGE:
                                try {
                                    ProjectPage imagePage = fileUtil.createImagePage(project.getId(),
                                            filePath, file.getOriginalFilename());
                                    pages.add(imagePage);
                                    pagesCreated = 1;
                                } catch (Exception e) {
                                    log.error("Failed to process image file {}: {}", file.getOriginalFilename(), e.getMessage());
                                    throw e;
                                }
                                break;

                            default:
                                log.warn("Unsupported file type: {} for file: {}", fileType, file.getOriginalFilename());
                                continue;
                        }

                        try {
                            for (ProjectPage page : pages) {
                                page.setPageNumber(totalPagesCreated + page.getPageNumber());
                                page.setReactionCount(0L);
                                page.setCommentCount(0L);
                            }

                            for (ProjectPage page : pages) {
                                projectPageMapper.insertProjectPage(page);
                                log.debug("Inserted page {} for project {}",
                                        page.getPageNumber(), project.getId());
                            }

                            totalPagesCreated += pagesCreated;
                            log.info("Successfully processed file {} and created {} pages (total: {})",
                                    file.getOriginalFilename(), pagesCreated, totalPagesCreated);

                        } catch (Exception e) {
                            log.error("Failed to save pages to database for file {}: {}",
                                    file.getOriginalFilename(), e.getMessage());
                            throw new RuntimeException("Failed to save pages to database: " + e.getMessage(), e);
                        }

                    } catch (Exception e) {
                        log.error("Failed to process file {}: {}", file.getOriginalFilename(), e.getMessage());
                        throw new RuntimeException("File processing failed for " + file.getOriginalFilename() + ": " + e.getMessage(), e);
                    }
                }
            }

            if (totalPagesCreated > 0 || (files != null && !files.isEmpty())) {
                try {
                    Project updateProject = new Project();
                    updateProject.setId(project.getId());
                    updateProject.setTotalPages(totalPagesCreated);
                    updateProject.setUpdatedAt(LocalDateTime.now());

                    if (totalPagesCreated > 0) {
                        updateProject.setStatus(ProjectStatus.ACTIVE);
                    }

                    int updatedRows = projectMapper.updateProject(updateProject);
                    if (updatedRows == 0) {
                        throw new RuntimeException("Failed to update project - no rows affected");
                    }

                    log.info("Updated project {} with {} actual pages and status", project.getId(), totalPagesCreated);
                } catch (Exception e) {
                    log.error("Failed to update project {}: {}", project.getId(), e.getMessage());
                    throw new RuntimeException("Failed to update project: " + e.getMessage(), e);
                }
            }

            ProjectResponse response;
            try {
                response = projectMapper.getProjectById(project.getId());
                if (response == null) {
                    throw new RuntimeException("Failed to retrieve created project");
                }
                enhanceProjectWithEngagementData(response, userId);
            } catch (Exception e) {
                log.error("Failed to retrieve project {}: {}", project.getId(), e.getMessage());
                throw new RuntimeException("Failed to retrieve created project: " + e.getMessage(), e);
            }

            log.info("Successfully created project {} with {} total pages in correct order",
                    project.getId(), totalPagesCreated);
            return new DataResponse<>(SUCCESS, "Project created successfully with file uploads",
                    HttpStatus.CREATED.value(), response);

        } catch (Exception e) {
            log.error("Failed to create project: {}", e.getMessage(), e);
            throw e;
        }
    }

    // Enhanced joinProject method
    @Override
    @Transactional
    public DataResponse<String> joinProject(String projectSlug, JoinProjectRequest request) {
        try {
            Long userId = getCurrentUserId();

            Project project = projectMapper.getProjectBySlug(projectSlug);
            if (project == null) {
                throw new DataNotFoundException();
            }

            if (project.getStatus() != ProjectStatus.ACTIVE) {
                return new DataResponse<>("Error", "Project is not currently accepting new members",
                        HttpStatus.BAD_REQUEST.value(), null);
            }

            ProjectMember existingMember = projectMemberMapper.getProjectMemberByProjectIdAndUserId(project.getId(), userId);
            if (existingMember != null) {
                return new DataResponse<>("Error", "You are already a member of this project",
                        HttpStatus.CONFLICT.value(), null);
            }

            ProjectRole role;
            try {
                role = ProjectRole.valueOf(String.valueOf(request.getRole()).toUpperCase());
            } catch (IllegalArgumentException e) {
                return new DataResponse<>("Error", "Invalid role specified",
                        HttpStatus.BAD_REQUEST.value(), null);
            }

            List<Integer> pageNumbers = request.getPageNumbers();
            if (pageNumbers != null && !pageNumbers.isEmpty()) {
                for (Integer pageNumber : pageNumbers) {
                    ProjectPage page = projectPageMapper.getProjectPageByProjectIdAndPageNumber(project.getId(), pageNumber);
                    if (page == null) {
                        return new DataResponse<>("Error",
                                "Page " + pageNumber + " does not exist in this project",
                                HttpStatus.BAD_REQUEST.value(), null);
                    }

                    if (isPageAlreadyAssignedForRole(project.getId(), pageNumber, role)) {
                        return new DataResponse<>("Error",
                                "Page " + pageNumber + " is already assigned to another " + role.name().toLowerCase(),
                                HttpStatus.CONFLICT.value(), null);
                    }
                }

                if (pageNumbers.stream().anyMatch(p -> p < 1 || p > project.getTotalPages())) {
                    return new DataResponse<>("Error",
                            "Some page numbers are outside the valid range (1-" + project.getTotalPages() + ")",
                            HttpStatus.BAD_REQUEST.value(), null);
                }
            }

            String validationError = validateRoleSpecificRequirements(role, request, project);
            if (validationError != null) {
                return new DataResponse<>("Error", validationError,
                        HttpStatus.BAD_REQUEST.value(), null);
            }

            ProjectMember projectMember = new ProjectMember();
            projectMember.setProjectId(project.getId());
            projectMember.setUserId(userId);
            projectMember.setRole(role);
            projectMember.setJoinedAt(LocalDateTime.now());
            projectMember.setIsActive(true);

            if (pageNumbers != null && !pageNumbers.isEmpty()) {
                projectMember.setAssignedPages(pageNumbers.toString());
                updatePageAssignments(project.getId(), userId, role, pageNumbers);
            } else if (role == ProjectRole.PROJECT_MANAGER) {
                projectMember.setAssignedPages("ALL");
            }

            projectMemberMapper.insertProjectMember(projectMember);
            updateProjectMemberCount(project.getId());

            // Auto-follow project when joining
            followProject(project.getId(), userId);

            // Create notification for project creator about new member
            createNotification(project.getCreatedBy(),
                    "New member joined your project '" + project.getTitle() + "' as " + role.name().toLowerCase(),
                    NotificationType.MEMBER_JOINED, project.getId(), null);

            log.info("User {} successfully joined project {} as {} with pages: {}",
                    userId, project.getId(), role, pageNumbers);

            String successMessage = String.format("Successfully joined project as %s",
                    role.name().toLowerCase().replace("_", " "));
            if (pageNumbers != null && !pageNumbers.isEmpty()) {
                successMessage += " with " + pageNumbers.size() + " assigned page(s)";
            }

            return new DataResponse<>(SUCCESS, successMessage, HttpStatus.OK.value(), "Project joined successfully");

        } catch (Exception e) {
            log.error("Failed to join project {}: {}", projectSlug, e.getMessage(), e);
            return new DataResponse<>("Error", "Failed to join project: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), null);
        }
    }

    // Enhanced submitWork method
    @Override
    @Transactional
    public DataResponse<String> submitWork(String projectSlug, Integer page, SubmitWorkRequest request) {
        try {
            Long userId = getCurrentUserId();

            Project project = projectMapper.getProjectBySlug(projectSlug);
            if (project == null) {
                return new DataResponse<>("Error", "Project not found",
                        HttpStatus.NOT_FOUND.value(), null);
            }

            ProjectMember projectMember = projectMemberMapper.getProjectMemberByProjectIdAndUserId(project.getId(), userId);
            if (projectMember == null || !projectMember.getIsActive()) {
                return new DataResponse<>("Error", "You are not an active member of this project",
                        HttpStatus.FORBIDDEN.value(), null);
            }

            ProjectPage projectPage = projectPageMapper.getProjectPageByProjectIdAndPageNumber(
                    project.getId(), page);
            if (projectPage == null) {
                return new DataResponse<>("Error",
                        "Page " + page + " does not exist in this project",
                        HttpStatus.BAD_REQUEST.value(), null);
            }

            if (!isUserAssignedToPage(projectMember, page)) {
                return new DataResponse<>("Error",
                        "You are not assigned to work on page " + page,
                        HttpStatus.FORBIDDEN.value(), null);
            }

            ProjectRole userRole = projectMember.getRole();
            boolean workSubmitted = false;

            switch (userRole) {
                case TRANSLATOR:
                    projectPageMapper.updateTranslationContent(project.getId(), page, request.getWorkContent(), userId, request.getNotes());
                    if (request.getIsCompleted()) {
                        projectPageMapper.markTranslationCompleted(project.getId(), page, userId);
                        updateProjectTranslationProgress(project.getId());
                    }
                    workSubmitted = true;
                    break;

                case EDITOR:
                    projectPageMapper.updateEditingContent(project.getId(), page,
                            request.getWorkContent(), userId, request.getNotes());
                    if (request.getIsCompleted()) {
                        projectPageMapper.markEditingCompleted(project.getId(), page, userId);
                        updateProjectEditingProgress(project.getId());
                    }
                    workSubmitted = true;
                    break;

                case ILLUSTRATOR:
                    projectPageMapper.updateIllustrationContent(project.getId(), page,
                            request.getWorkContent(), userId, request.getNotes());
                    if (request.getIsCompleted()) {
                        projectPageMapper.markIllustrationCompleted(project.getId(), page, userId);
                        updateProjectIllustrationProgress(project.getId());
                    }
                    workSubmitted = true;
                    break;

                case PROOFREADER:
                    projectPageMapper.updateProofreadingContent(project.getId(), page,
                            request.getWorkContent(), userId, request.getNotes());
                    if (request.getIsCompleted()) {
                        projectPageMapper.markProofreadingCompleted(project.getId(), page, userId);
                        updateProjectProofreadingProgress(project.getId());
                    }
                    workSubmitted = true;
                    break;

                case CONTENT_PROVIDER:
                    projectPageMapper.updateTranscriptionContent(project.getId(), page,
                            request.getWorkContent(), userId, request.getNotes());
                    if (request.getIsCompleted()) {
                        projectPageMapper.markTranscriptionCompleted(project.getId(), page, userId);
                        updateProjectTranscriptionProgress(project.getId());
                    }
                    workSubmitted = true;
                    break;

                case REVIEWER:
                    projectPageMapper.updateReviewContent(project.getId(), page,
                            request.getWorkContent(), userId, request.getNotes());
                    if (request.getIsCompleted()) {
                        projectPageMapper.markReviewCompleted(project.getId(), page, userId);
                    }
                    workSubmitted = true;
                    break;

                default:
                    return new DataResponse<>("Error",
                            "Your role (" + userRole + ") is not authorized to submit work",
                            HttpStatus.FORBIDDEN.value(), null);
            }

            if (workSubmitted) {
                projectPageMapper.updatePageLastModified(project.getId(), page, LocalDateTime.now());
                updateProjectOverallProgress(project.getId());
                projectMapper.updateProjectTimestamp(project.getId(), LocalDateTime.now());

                // Create notifications for project followers about work submission
                if (request.getIsCompleted()) {
                    notifyProjectFollowers(project.getId(), userId,
                            "Work completed on page " + page + " by " + userRole.name().toLowerCase(),
                            NotificationType.WORK_COMPLETED, page);
                }

                log.info("User {} successfully submitted work for page {} in project {} as {}",
                        userId, page, project.getId(), userRole);

                String successMessage = String.format("Work submitted successfully for page %d as %s",
                        page, userRole.name().toLowerCase().replace("_", " "));

                if (request.getIsCompleted()) {
                    successMessage += " and marked as completed";
                }

                return new DataResponse<>(SUCCESS, successMessage, HttpStatus.OK.value(), "Work submitted successfully");
            }

            return new DataResponse<>("Error", "Failed to submit work",
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), null);

        } catch (Exception e) {
            log.error("Failed to submit work for project {} page {}: {}",
                    projectSlug, page, e.getMessage(), e);
            return new DataResponse<>("Error", "Failed to submit work: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), null);
        }
    }

    // NEW FEATURE: Project Reaction Management
    @Override
    @Transactional
    public DataResponse<ProjectReactionResponse> reactToProject(String projectSlug, ProjectReactionRequest request) {
        try {
            Long userId = getCurrentUserId();

            Project project = projectMapper.getProjectBySlug(projectSlug);
            if (project == null) {
                return new DataResponse<>("Error", "Project not found",
                        HttpStatus.NOT_FOUND.value(), null);
            }

            ReactionType reactionType;
            try {
                reactionType = ReactionType.valueOf(request.getReactionType().toUpperCase());
            } catch (IllegalArgumentException e) {
                return new DataResponse<>("Error", "Invalid reaction type",
                        HttpStatus.BAD_REQUEST.value(), null);
            }

            // Check if user already reacted to this project
            ProjectReaction existingReaction = projectReactionMapper.getProjectReactionByUserAndProject(userId, project.getId());

            if (existingReaction != null) {
                if (existingReaction.getReactionType() == reactionType) {
                    // Remove reaction if same type
                    projectReactionMapper.deleteProjectReaction(existingReaction.getId());
                    updateProjectReactionCount(project.getId());

                    return new DataResponse<>(SUCCESS, "Reaction removed",
                            HttpStatus.OK.value(), null);
                } else {
                    // Update reaction type
                    existingReaction.setReactionType(reactionType);
                    existingReaction.setUpdatedAt(LocalDateTime.now());
                    projectReactionMapper.updateProjectReaction(existingReaction);

                    ProjectReactionResponse response = new ProjectReactionResponse();
                    response.setReactionType(reactionType.name());
                    response.setCreatedAt(existingReaction.getCreatedAt());
                    response.setUpdatedAt(existingReaction.getUpdatedAt());

                    return new DataResponse<>(SUCCESS, "Reaction updated",
                            HttpStatus.OK.value(), response);
                }
            } else {
                // Create new reaction
                ProjectReaction newReaction = new ProjectReaction();
                newReaction.setProjectId(project.getId());
                newReaction.setUserId(userId);
                newReaction.setReactionType(reactionType);
                newReaction.setCreatedAt(LocalDateTime.now());
                newReaction.setUpdatedAt(LocalDateTime.now());

                projectReactionMapper.insertProjectReaction(newReaction);
                updateProjectReactionCount(project.getId());

                // Create notification for project creator
                if (!userId.equals(project.getCreatedBy())) {
                    createNotification(project.getCreatedBy(),
                            "Someone reacted to your project '" + project.getTitle() + "' with " + reactionType.getDisplayName(),
                            NotificationType.PROJECT_REACTION, project.getId(), null);
                }

                ProjectReactionResponse response = new ProjectReactionResponse();
                response.setReactionType(reactionType.name());
                response.setCreatedAt(newReaction.getCreatedAt());
                response.setUpdatedAt(newReaction.getUpdatedAt());

                return new DataResponse<>(SUCCESS, "Reaction added",
                        HttpStatus.CREATED.value(), response);
            }

        } catch (Exception e) {
            log.error("Failed to react to project {}: {}", projectSlug, e.getMessage(), e);
            return new DataResponse<>("Error", "Failed to process reaction: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), null);
        }
    }

    // NEW FEATURE: Project Comment Management
    @Override
    @Transactional
    public DataResponse<ProjectCommentResponse> addProjectComment(String projectSlug, ProjectCommentRequest request) {
        try {
            Long userId = getCurrentUserId();

            Project project = projectMapper.getProjectBySlug(projectSlug);
            if (project == null) {
                return new DataResponse<>("Error", "Project not found",
                        HttpStatus.NOT_FOUND.value(), null);
            }

            // Validate comment content
            if (request.getContent() == null || request.getContent().trim().isEmpty()) {
                return new DataResponse<>("Error", "Comment content cannot be empty",
                        HttpStatus.BAD_REQUEST.value(), null);
            }

            if (request.getContent().length() > 2000) {
                return new DataResponse<>("Error", "Comment content exceeds maximum length of 2000 characters",
                        HttpStatus.BAD_REQUEST.value(), null);
            }

            ProjectComment comment = new ProjectComment();
            comment.setProjectId(project.getId());
            comment.setUserId(userId);
            comment.setContent(request.getContent().trim());
            comment.setParentCommentId(request.getParentCommentId());
            comment.setCreatedAt(LocalDateTime.now());
            comment.setUpdatedAt(LocalDateTime.now());
            comment.setIsEdited(false);
            comment.setIsDeleted(false);
            comment.setReactionCount(0L);

            projectCommentMapper.insertProjectComment(comment);
            updateProjectCommentCount(project.getId());

            // Create notification for project creator and parent comment author
            if (!userId.equals(project.getCreatedBy())) {
                createNotification(project.getCreatedBy(),
                        "New comment on your project '" + project.getTitle() + "'",
                        NotificationType.PROJECT_COMMENT, project.getId(), null);
            }

            // If this is a reply, notify the parent comment author
            if (request.getParentCommentId() != null) {
                ProjectComment parentComment = projectCommentMapper.getProjectCommentById(request.getParentCommentId());
                if (parentComment != null && !userId.equals(parentComment.getUserId())) {
                    createNotification(parentComment.getUserId(),
                            "Someone replied to your comment on '" + project.getTitle() + "'",
                            NotificationType.COMMENT_REPLY, project.getId(), null);
                }
            }

            // Get complete comment response with user info
            ProjectCommentResponse response = projectCommentMapper.getProjectCommentWithUserInfo(comment.getId());

            log.info("User {} added comment to project {}", userId, project.getId());

            return new DataResponse<>(SUCCESS, "Comment added successfully",
                    HttpStatus.CREATED.value(), response);

        } catch (Exception e) {
            log.error("Failed to add comment to project {}: {}", projectSlug, e.getMessage(), e);
            return new DataResponse<>("Error", "Failed to add comment: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), null);
        }
    }

    // NEW FEATURE: Page Reaction Management
    @Override
    @Transactional
    public DataResponse<PageReactionResponse> reactToPage(String projectSlug, Integer pageNumber, PageReactionRequest request) {
        try {
            Long userId = getCurrentUserId();

            Project project = projectMapper.getProjectBySlug(projectSlug);
            if (project == null) {
                return new DataResponse<>("Error", "Project not found",
                        HttpStatus.NOT_FOUND.value(), null);
            }

            ProjectPage projectPage = projectPageMapper.getProjectPageByProjectIdAndPageNumber(project.getId(), pageNumber);
            if (projectPage == null) {
                return new DataResponse<>("Error", "Page not found",
                        HttpStatus.NOT_FOUND.value(), null);
            }

            ReactionType reactionType;
            try {
                reactionType = ReactionType.valueOf(request.getReactionType().toUpperCase());
            } catch (IllegalArgumentException e) {
                return new DataResponse<>("Error", "Invalid reaction type",
                        HttpStatus.BAD_REQUEST.value(), null);
            }

            PageReaction existingReaction = pageReactionMapper.getPageReactionByUserAndPage(userId, projectPage.getId());

            if (existingReaction != null) {
                if (existingReaction.getReactionType() == reactionType) {
                    // Remove reaction if same type
                    pageReactionMapper.deletePageReaction(existingReaction.getId());
                    updatePageReactionCount(projectPage.getId());

                    return new DataResponse<>(SUCCESS, "Reaction removed",
                            HttpStatus.OK.value(), null);
                } else {
                    // Update reaction type
                    existingReaction.setReactionType(reactionType);
                    existingReaction.setUpdatedAt(LocalDateTime.now());
                    pageReactionMapper.updatePageReaction(existingReaction);

                    PageReactionResponse response = new PageReactionResponse();
                    response.setReactionType(reactionType.name());
                    response.setCreatedAt(existingReaction.getCreatedAt());
                    response.setUpdatedAt(existingReaction.getUpdatedAt());

                    return new DataResponse<>(SUCCESS, "Reaction updated",
                            HttpStatus.OK.value(), response);
                }
            } else {
                // Create new reaction
                PageReaction newReaction = new PageReaction();
                newReaction.setPageId(projectPage.getId());
                newReaction.setUserId(userId);
                newReaction.setReactionType(reactionType);
                newReaction.setCreatedAt(LocalDateTime.now());
                newReaction.setUpdatedAt(LocalDateTime.now());

                pageReactionMapper.insertPageReaction(newReaction);
                updatePageReactionCount(projectPage.getId());

                // Get page contributor to notify
                Long pageContributorId = getPageContributorId(projectPage);
                if (pageContributorId != null && !userId.equals(pageContributorId)) {
                    createNotification(pageContributorId,
                            "Someone reacted to your work on page " + pageNumber + " with " + reactionType.getDisplayName(),
                            NotificationType.PAGE_REACTION, project.getId(), pageNumber);
                }

                PageReactionResponse response = new PageReactionResponse();
                response.setReactionType(reactionType.name());
                response.setCreatedAt(newReaction.getCreatedAt());
                response.setUpdatedAt(newReaction.getUpdatedAt());

                return new DataResponse<>(SUCCESS, "Reaction added",
                        HttpStatus.CREATED.value(), response);
            }

        } catch (Exception e) {
            log.error("Failed to react to page {} in project {}: {}", pageNumber, projectSlug, e.getMessage(), e);
            return new DataResponse<>("Error", "Failed to process reaction: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), null);
        }
    }

    // NEW FEATURE: Page Comment Management
    @Override
    @Transactional
    public DataResponse<PageCommentResponse> addPageComment(String projectSlug, Integer pageNumber, PageCommentRequest request) {
        try {
            Long userId = getCurrentUserId();

            Project project = projectMapper.getProjectBySlug(projectSlug);
            if (project == null) {
                return new DataResponse<>("Error", "Project not found",
                        HttpStatus.NOT_FOUND.value(), null);
            }

            ProjectPage projectPage = projectPageMapper.getProjectPageByProjectIdAndPageNumber(project.getId(), pageNumber);
            if (projectPage == null) {
                return new DataResponse<>("Error", "Page not found",
                        HttpStatus.NOT_FOUND.value(), null);
            }

            // Validate comment content
            if (request.getContent() == null || request.getContent().trim().isEmpty()) {
                return new DataResponse<>("Error", "Comment content cannot be empty",
                        HttpStatus.BAD_REQUEST.value(), null);
            }

            if (request.getContent().length() > 1500) {
                return new DataResponse<>("Error", "Comment content exceeds maximum length of 1500 characters",
                        HttpStatus.BAD_REQUEST.value(), null);
            }

            PageComment comment = new PageComment();
            comment.setPageId(projectPage.getId());
            comment.setUserId(userId);
            comment.setContent(request.getContent().trim());
            comment.setCommentType(CommentType.valueOf(request.getCommentType().toUpperCase()));
            comment.setParentCommentId(request.getParentCommentId());
            comment.setCreatedAt(LocalDateTime.now());
            comment.setUpdatedAt(LocalDateTime.now());
            comment.setIsEdited(false);
            comment.setIsDeleted(false);
            comment.setReactionCount(0L);

            pageCommentMapper.insertPageComment(comment);
            updatePageCommentCount(projectPage.getId());

            // Create notifications
            Long pageContributorId = getPageContributorId(projectPage);
            if (pageContributorId != null && !userId.equals(pageContributorId)) {
                createNotification(pageContributorId,
                        "New comment on your work for page " + pageNumber,
                        NotificationType.PAGE_COMMENT, project.getId(), pageNumber);
            }

            // If this is a reply, notify the parent comment author
            if (request.getParentCommentId() != null) {
                PageComment parentComment = pageCommentMapper.getPageCommentById(request.getParentCommentId());
                if (parentComment != null && !userId.equals(parentComment.getUserId())) {
                    createNotification(parentComment.getUserId(),
                            "Someone replied to your comment on page " + pageNumber,
                            NotificationType.COMMENT_REPLY, project.getId(), pageNumber);
                }
            }

            // Get complete comment response with user info
            PageCommentResponse response = pageCommentMapper.getPageCommentWithUserInfo(comment.getId());

            log.info("User {} added comment to page {} in project {}", userId, pageNumber, project.getId());

            return new DataResponse<>(SUCCESS, "Comment added successfully",
                    HttpStatus.CREATED.value(), response);

        } catch (Exception e) {
            log.error("Failed to add comment to page {} in project {}: {}", pageNumber, projectSlug, e.getMessage(), e);
            return new DataResponse<>("Error", "Failed to add comment: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), null);
        }
    }

    // NEW FEATURE: Get Project Comments
    @Override
    public DatatableResponse<ProjectCommentResponse> getProjectComments(String projectSlug, int page, int limit, String sortOrder) {
        try {
            Project project = projectMapper.getProjectBySlug(projectSlug);
            if (project == null) {
                return new DatatableResponse<>("Error", "Project not found", HttpStatus.NOT_FOUND.value(), null);
            }

            String sortType = Objects.equals(sortOrder, "ASC") ? "ASC" : "DESC";
            int offset = (page - 1) * limit;

            List<ProjectCommentResponse> comments = projectCommentMapper.getProjectCommentsWithUserInfo(
                    project.getId(), offset, limit, sortType);

            Long currentUserId = getCurrentUserId();
            for (ProjectCommentResponse comment : comments) {
                enhanceCommentWithEngagementData(comment, currentUserId);
            }

            PageDataResponse<ProjectCommentResponse> data = new PageDataResponse<>(page, limit, comments.size(), comments);
            return new DatatableResponse<>(SUCCESS, "Comments fetched successfully", HttpStatus.OK.value(), data);

        } catch (Exception e) {
            log.error("Failed to get comments for project {}: {}", projectSlug, e.getMessage(), e);
            return new DatatableResponse<>("Error", "Failed to fetch comments: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), null);
        }
    }

    // NEW FEATURE: Get Page Comments
    @Override
    public DatatableResponse<PageCommentResponse> getPageComments(String projectSlug, Integer pageNumber, int page, int limit, String sortOrder) {
        try {
            Project project = projectMapper.getProjectBySlug(projectSlug);
            if (project == null) {
                return new DatatableResponse<>("Error", "Project not found", HttpStatus.NOT_FOUND.value(), null);
            }

            ProjectPage projectPage = projectPageMapper.getProjectPageByProjectIdAndPageNumber(project.getId(), pageNumber);
            if (projectPage == null) {
                return new DatatableResponse<>("Error", "Page not found", HttpStatus.NOT_FOUND.value(), null);
            }

            String sortType = Objects.equals(sortOrder, "ASC") ? "ASC" : "DESC";
            int offset = (page - 1) * limit;

            List<PageCommentResponse> comments = pageCommentMapper.getPageCommentsWithUserInfo(
                    projectPage.getId(), offset, limit, sortType);

            Long currentUserId = getCurrentUserId();
            for (PageCommentResponse comment : comments) {
                enhancePageCommentWithEngagementData(comment, currentUserId);
            }

            PageDataResponse<PageCommentResponse> data = new PageDataResponse<>(page, limit, comments.size(), comments);
            return new DatatableResponse<>(SUCCESS, "Page comments fetched successfully", HttpStatus.OK.value(), data);

        } catch (Exception e) {
            log.error("Failed to get comments for page {} in project {}: {}", pageNumber, projectSlug, e.getMessage(), e);
            return new DatatableResponse<>("Error", "Failed to fetch page comments: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), null);
        }
    }

    // NEW FEATURE: Project Following System
    @Override
    @Transactional
    public DataResponse<String> followProject(String projectSlug) {
        try {
            Long userId = getCurrentUserId();

            Project project = projectMapper.getProjectBySlug(projectSlug);
            if (project == null) {
                return new DataResponse<>("Error", "Project not found",
                        HttpStatus.NOT_FOUND.value(), null);
            }

            // Check if already following
            ProjectFollow existingFollow = projectFollowMapper.getProjectFollowByUserAndProject(userId, project.getId());
            if (existingFollow != null) {
                if (Boolean.TRUE.equals(existingFollow.getIsActive())) {
                    return new DataResponse<>("Error", "You are already following this project",
                            HttpStatus.CONFLICT.value(), null);
                } else {
                    // Reactivate follow
                    projectFollowMapper.reactivateProjectFollow(existingFollow.getId());
                }
            } else {
                // Create new follow
                followProject(project.getId(), userId);
            }

            updateProjectFollowerCount(project.getId());

            // Create notification for project creator
            if (!userId.equals(project.getCreatedBy())) {
                createNotification(project.getCreatedBy(),
                        "Someone started following your project '" + project.getTitle() + "'",
                        NotificationType.PROJECT_FOLLOW, project.getId(), null);
            }

            return new DataResponse<>(SUCCESS, "Successfully following project",
                    HttpStatus.OK.value(), "Project followed");

        } catch (Exception e) {
            log.error("Failed to follow project {}: {}", projectSlug, e.getMessage(), e);
            return new DataResponse<>("Error", "Failed to follow project: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), null);
        }
    }

    @Override
    @Transactional
    public DataResponse<String> unfollowProject(String projectSlug) {
        try {
            Long userId = getCurrentUserId();

            Project project = projectMapper.getProjectBySlug(projectSlug);
            if (project == null) {
                return new DataResponse<>("Error", "Project not found",
                        HttpStatus.NOT_FOUND.value(), null);
            }

            ProjectFollow existingFollow = projectFollowMapper.getProjectFollowByUserAndProject(userId, project.getId());
            if (existingFollow == null || !existingFollow.getIsActive()) {
                return new DataResponse<>("Error", "You are not following this project",
                        HttpStatus.BAD_REQUEST.value(), null);
            }

            projectFollowMapper.deactivateProjectFollow(existingFollow.getId());
            updateProjectFollowerCount(project.getId());

            return new DataResponse<>(SUCCESS, "Successfully unfollowed project",
                    HttpStatus.OK.value(), "Project unfollowed");

        } catch (Exception e) {
            log.error("Failed to unfollow project {}: {}", projectSlug, e.getMessage(), e);
            return new DataResponse<>("Error", "Failed to unfollow project: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), null);
        }
    }

    // NEW FEATURE: Get Project Statistics
    @Override
    public DataResponse<ProjectStatisticsResponse> getProjectStatistics(String projectSlug) {
        try {
            Project project = projectMapper.getProjectBySlug(projectSlug);
            if (project == null) {
                return new DataResponse<>("Error", "Project not found",
                        HttpStatus.NOT_FOUND.value(), null);
            }

            // Increment view count
            projectMapper.incrementViewCount(project.getId());

            ProjectStatisticsResponse stats = new ProjectStatisticsResponse();
            stats.setProjectId(project.getId());
            stats.setTotalPages(project.getTotalPages());
            stats.setCompletedPages(project.getPagesCompleted());
            stats.setOverallProgress(project.getOverallProgress());

            // Get detailed progress by work type
            stats.setTranscriptionProgress(calculateProgressPercentage(project.getTranscriptionCompletedPages(), project.getTotalPages()));
            stats.setTranslationProgress(calculateProgressPercentage(project.getTranslationCompletedPages(), project.getTotalPages()));
            stats.setEditingProgress(calculateProgressPercentage(project.getEditingCompletedPages(), project.getTotalPages()));
            stats.setIllustrationProgress(calculateProgressPercentage(project.getIllustrationCompletedPages(), project.getTotalPages()));
            stats.setProofreadingProgress(calculateProgressPercentage(
                    project.getProofreadingCompletedPages() != null ? project.getProofreadingCompletedPages() : 0,
                    project.getTotalPages()));

            // Get engagement statistics
            stats.setTotalMembers(projectMemberMapper.getActiveMemberCountByProjectId(project.getId()));
            stats.setTotalFollowers(projectFollowMapper.getActiveFollowerCountByProjectId(project.getId()));
            stats.setTotalReactions(projectReactionMapper.getProjectReactionCount(project.getId()));
            stats.setTotalComments(projectCommentMapper.getProjectCommentCount(project.getId()));
            stats.setViewCount(project.getViewCount() + 1); // Include the current view

            // Get reaction breakdown
            List<ReactionSummary> reactionBreakdown = projectReactionMapper.getProjectReactionBreakdown(project.getId());
            stats.setReactionBreakdown(reactionBreakdown);

            // Get member breakdown by role
            List<MemberRoleSummary> memberBreakdown = projectMemberMapper.getProjectMemberRoleBreakdown(project.getId());
            stats.setMemberBreakdown(memberBreakdown);

            // Get recent activity
            List<ProjectActivityResponse> recentActivity = getRecentProjectActivity(project.getId(), 10);
            stats.setRecentActivity(recentActivity);

            return new DataResponse<>(SUCCESS, "Statistics fetched successfully",
                    HttpStatus.OK.value(), stats);

        } catch (Exception e) {
            log.error("Failed to get statistics for project {}: {}", projectSlug, e.getMessage(), e);
            return new DataResponse<>("Error", "Failed to fetch statistics: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), null);
        }
    }

    // NEW FEATURE: Project Quality Assessment
    @Override
    @Transactional
    public DataResponse<String> assessProjectQuality(String projectSlug, ProjectQualityAssessmentRequest request) {
        try {
            Long userId = getCurrentUserId();

            Project project = projectMapper.getProjectBySlug(projectSlug);
            if (project == null) {
                return new DataResponse<>("Error", "Project not found",
                        HttpStatus.NOT_FOUND.value(), null);
            }

            // Check if user is a reviewer or project manager
            ProjectMember projectMember = projectMemberMapper.getProjectMemberByProjectIdAndUserId(project.getId(), userId);
            if (projectMember == null ||
                    (projectMember.getRole() != ProjectRole.REVIEWER && projectMember.getRole() != ProjectRole.PROJECT_MANAGER)) {
                return new DataResponse<>("Error", "Only reviewers and project managers can assess quality",
                        HttpStatus.FORBIDDEN.value(), null);
            }

            // Create quality assessment
            ProjectQualityAssessment assessment = new ProjectQualityAssessment();
            assessment.setProjectId(project.getId());
            assessment.setAssessorId(userId);
            assessment.setQualityScore(request.getQualityScore());
            assessment.setContentAccuracy(request.getContentAccuracy());
            assessment.setLanguageQuality(request.getLanguageQuality());
            assessment.setFormattingQuality(request.getFormattingQuality());
            assessment.setOverallReadability(request.getOverallReadability());
            assessment.setRecommendations(request.getRecommendations());
            assessment.setCreatedAt(LocalDateTime.now());

            projectMapper.insertQualityAssessment(assessment);

            // Update project quality score
            updateProjectQualityScore(project.getId());

            // Create notification for project creator
            if (!userId.equals(project.getCreatedBy())) {
                createNotification(project.getCreatedBy(),
                        "Quality assessment completed for your project '" + project.getTitle() + "'",
                        NotificationType.QUALITY_ASSESSMENT, project.getId(), null);
            }

            return new DataResponse<>(SUCCESS, "Quality assessment submitted successfully",
                    HttpStatus.OK.value(), "Assessment recorded");

        } catch (Exception e) {
            log.error("Failed to assess quality for project {}: {}", projectSlug, e.getMessage(), e);
            return new DataResponse<>("Error", "Failed to submit assessment: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), null);
        }
    }

    // NEW FEATURE: Mark Project as Complete
    @Override
    @Transactional
    public DataResponse<String> markProjectComplete(String projectSlug) {
        try {
            Long userId = getCurrentUserId();

            Project project = projectMapper.getProjectBySlug(projectSlug);
            if (project == null) {
                return new DataResponse<>("Error", "Project not found",
                        HttpStatus.NOT_FOUND.value(), null);
            }

            // Check if user is project creator or project manager
            if (!userId.equals(project.getCreatedBy())) {
                ProjectMember projectMember = projectMemberMapper.getProjectMemberByProjectIdAndUserId(project.getId(), userId);
                if (projectMember == null || projectMember.getRole() != ProjectRole.PROJECT_MANAGER) {
                    return new DataResponse<>("Error", "Only project creator or project manager can mark project as complete",
                            HttpStatus.FORBIDDEN.value(), null);
                }
            }

            // Check if project is ready to be marked complete
            if (project.getOverallProgress().compareTo(BigDecimal.valueOf(95)) < 0) {
                return new DataResponse<>("Error", "Project must be at least 95% complete to mark as finished",
                        HttpStatus.BAD_REQUEST.value(), null);
            }

            // Update project status
            Project updateProject = new Project();
            updateProject.setId(project.getId());
            updateProject.setStatus(ProjectStatus.COMPLETED);
            updateProject.setCompletedAt(LocalDateTime.now());
            updateProject.setUpdatedAt(LocalDateTime.now());

            projectMapper.updateProject(updateProject);

            // Notify all project members and followers
            notifyProjectMembersAndFollowers(project.getId(), userId,
                    "Project '" + project.getTitle() + "' has been marked as complete!",
                    NotificationType.PROJECT_COMPLETED);

            return new DataResponse<>(SUCCESS, "Project marked as complete successfully",
                    HttpStatus.OK.value(), "Project completed");

        } catch (Exception e) {
            log.error("Failed to mark project {} as complete: {}", projectSlug, e.getMessage(), e);
            return new DataResponse<>("Error", "Failed to mark project as complete: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), null);
        }
    }

    // NEW FEATURE: Export Project
//    @Override
//    public DataResponse<ProjectExportResponse> exportProject(String projectSlug, String format) {
//        try {
//            Long userId = getCurrentUserId();
//
//            Project project = projectMapper.getProjectBySlug(projectSlug);
//            if (project == null) {
//                return new DataResponse<>("Error", "Project not found",
//                        HttpStatus.NOT_FOUND.value(), null);
//            }
//
//            // Check if project is complete or user has permission
//            if (project.getStatus() != ProjectStatus.COMPLETED && !userId.equals(project.getCreatedBy())) {
//                ProjectMember projectMember = projectMemberMapper.getProjectMemberByProjectIdAndUserId(project.getId(), userId);
//                if (projectMember == null || projectMember.getRole() != ProjectRole.PROJECT_MANAGER) {
//                    return new DataResponse<>("Error", "Only completed projects can be exported by non-creators",
//                            HttpStatus.FORBIDDEN.value(), null);
//                }
//            }
//
//            ExportFormat exportFormat;
//            try {
//                exportFormat = ExportFormat.valueOf(format.toUpperCase());
//            } catch (IllegalArgumentException e) {
//                return new DataResponse<>("Error", "Invalid export format. Supported formats: PDF, EPUB, HTML, DOCX",
//                        HttpStatus.BAD_REQUEST.value(), null);
//            }
//
//            // Get all project pages with content
//            List<ProjectPageExportData> pages = projectPageMapper.getProjectPagesForExport(project.getId());
//
//            // Generate export file
//            ProjectExportResponse exportResponse = generateProjectExport(project, pages, exportFormat);
//
//            // Log export activity
//            logProjectActivity(project.getId(), userId, ProjectActivityType.EXPORT,
//                    "Project exported in " + format + " format");
//
//            return new DataResponse<>(SUCCESS, "Project exported successfully",
//                    HttpStatus.OK.value(), exportResponse);
//
//        } catch (Exception e) {
//            log.error("Failed to export project {}: {}", projectSlug, e.getMessage(), e);
//            return new DataResponse<>("Error", "Failed to export project: " + e.getMessage(),
//                    HttpStatus.INTERNAL_SERVER_ERROR.value(), null);
//        }
//    }

    // Helper methods for new features

    private void enhanceProjectWithEngagementData(ProjectResponse project, Long currentUserId) {
        try {
            // Get reaction counts
            List<ReactionSummary> reactions = projectReactionMapper.getProjectReactionBreakdown(project.getId());
            project.setReactionBreakdown(reactions);

            // Check if current user has reacted
            if (currentUserId != null) {
                ProjectReaction userReaction = projectReactionMapper.getProjectReactionByUserAndProject(currentUserId, project.getId());
                project.setUserReaction(userReaction != null ? userReaction.getReactionType().name() : null);

                // Check if user is following
                ProjectFollow userFollow = projectFollowMapper.getProjectFollowByUserAndProject(currentUserId, project.getId());
                project.setIsFollowing(userFollow.getIsActive());
            }

            // Get recent comments preview
            List<ProjectCommentResponse> recentComments = projectCommentMapper.getProjectCommentsWithUserInfo(
                    project.getId(), 0, 3, "DESC");
            project.setRecentComments(recentComments);

        } catch (Exception e) {
            log.warn("Failed to enhance project {} with engagement data: {}", project.getId(), e.getMessage());
        }
    }

    private void enhanceCommentWithEngagementData(ProjectCommentResponse comment, Long currentUserId) {
        try {
            // Get reply count
            int replyCount = projectCommentMapper.getCommentReplyCount(comment.getId());
            comment.setReplyCount(replyCount);

            // Get recent replies
            if (replyCount > 0) {
                List<ProjectCommentResponse> replies = projectCommentMapper.getCommentReplies(comment.getId(), 0, 3);
                comment.setReplies(replies);
            }

        } catch (Exception e) {
            log.warn("Failed to enhance comment {} with engagement data: {}", comment.getId(), e.getMessage());
        }
    }

    private void enhancePageCommentWithEngagementData(PageCommentResponse comment, Long currentUserId) {
        try {
            // Get reply count
            int replyCount = pageCommentMapper.getPageCommentReplyCount(comment.getId());
            comment.setReplyCount(replyCount);

            // Get recent replies
            if (replyCount > 0) {
                List<PageCommentResponse> replies = pageCommentMapper.getPageCommentReplies(comment.getId(), 0, 3);
                comment.setReplies(replies);
            }

        } catch (Exception e) {
            log.warn("Failed to enhance page comment {} with engagement data: {}", comment.getId(), e.getMessage());
        }
    }

    private void followProject(Long projectId, Long userId) {
        try {
            ProjectFollow follow = new ProjectFollow();
            follow.setProjectId(projectId);
            follow.setUserId(userId);
            follow.setFollowedAt(LocalDateTime.now());
            follow.setIsActive(true);

            projectFollowMapper.insertProjectFollow(follow);
        } catch (Exception e) {
            log.warn("Failed to create follow relationship for user {} and project {}: {}",
                    userId, projectId, e.getMessage());
        }
    }

    private void createNotification(Long recipientId, String message, NotificationType type,
                                    Long projectId, Integer pageNumber) {
        try {
            Notification notification = new Notification();
            notification.setUserId(recipientId);
            notification.setMessage(message);
            notification.setType(type);
            notification.setProjectId(projectId);
            notification.setPageNumber(pageNumber);
            notification.setIsRead(false);
            notification.setCreatedAt(LocalDateTime.now());

            notificationMapper.insertNotification(notification);
        } catch (Exception e) {
            log.warn("Failed to create notification for user {}: {}", recipientId, e.getMessage());
        }
    }

    private void notifyProjectFollowers(Long projectId, Long excludeUserId, String message,
                                        NotificationType type, Integer pageNumber) {
        try {
            List<Long> followerIds = projectFollowMapper.getActiveFollowerIdsByProjectId(projectId);
            for (Long followerId : followerIds) {
                if (!followerId.equals(excludeUserId)) {
                    createNotification(followerId, message, type, projectId, pageNumber);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to notify project {} followers: {}", projectId, e.getMessage());
        }
    }

    private void notifyProjectMembersAndFollowers(Long projectId, Long excludeUserId, String message, NotificationType type) {
        try {
            // Notify members
            List<Long> memberIds = projectMemberMapper.getActiveMemberIdsByProjectId(projectId);
            for (Long memberId : memberIds) {
                if (!memberId.equals(excludeUserId)) {
                    createNotification(memberId, message, type, projectId, null);
                }
            }

            // Notify followers
            List<Long> followerIds = projectFollowMapper.getActiveFollowerIdsByProjectId(projectId);
            for (Long followerId : followerIds) {
                if (!followerId.equals(excludeUserId) && !memberIds.contains(followerId)) {
                    createNotification(followerId, message, type, projectId, null);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to notify project {} members and followers: {}", projectId, e.getMessage());
        }
    }

    private void updateProjectReactionCount(Long projectId) {
        try {
            Long reactionCount = projectReactionMapper.getProjectReactionCount(projectId);
            projectMapper.updateReactionCount(projectId, reactionCount);
        } catch (Exception e) {
            log.warn("Failed to update reaction count for project {}: {}", projectId, e.getMessage());
        }
    }

    private void updateProjectCommentCount(Long projectId) {
        try {
            Long commentCount = projectCommentMapper.getProjectCommentCount(projectId);
            projectMapper.updateCommentCount(projectId, commentCount);
        } catch (Exception e) {
            log.warn("Failed to update comment count for project {}: {}", projectId, e.getMessage());
        }
    }

    private void updatePageReactionCount(Long pageId) {
        try {
            Long reactionCount = pageReactionMapper.getPageReactionCount(pageId);
            projectPageMapper.updatePageReactionCount(pageId, reactionCount);
        } catch (Exception e) {
            log.warn("Failed to update reaction count for page {}: {}", pageId, e.getMessage());
        }
    }

    private void updatePageCommentCount(Long pageId) {
        try {
            Long commentCount = pageCommentMapper.getPageCommentCount(pageId);
            projectPageMapper.updatePageCommentCount(pageId, commentCount);
        } catch (Exception e) {
            log.warn("Failed to update comment count for page {}: {}", pageId, e.getMessage());
        }
    }

    private void updateProjectFollowerCount(Long projectId) {
        try {
            Long followerCount = projectFollowMapper.getActiveFollowerCountByProjectId(projectId);
            projectMapper.updateFollowerCount(projectId, followerCount);
        } catch (Exception e) {
            log.warn("Failed to update follower count for project {}: {}", projectId, e.getMessage());
        }
    }

    private Long getPageContributorId(ProjectPage projectPage) {
        // Check which role contributed to this page
        if (projectPage.getTranslationCompletedBy() != null) {
            return projectPage.getTranslationCompletedBy();
        } else if (projectPage.getEditingCompletedBy() != null) {
            return projectPage.getEditingCompletedBy();
        } else if (projectPage.getIllustrationCompletedBy() != null) {
            return projectPage.getIllustrationCompletedBy();
        } else if (projectPage.getTranscriptionCompletedBy() != null) {
            return projectPage.getTranscriptionCompletedBy();
        } else if (projectPage.getProofreadingCompletedBy() != null) {
            return projectPage.getProofreadingCompletedBy();
        }
        return null;
    }

    private BigDecimal calculateProgressPercentage(Integer completed, Integer total) {
        if (total == null || total == 0) {
            return BigDecimal.ZERO;
        }
        if (completed == null) {
            completed = 0;
        }
        return BigDecimal.valueOf(completed)
                .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    private List<ProjectActivityResponse> getRecentProjectActivity(Long projectId, int limit) {
        try {
            return projectMapper.getRecentProjectActivity(projectId, limit);
        } catch (Exception e) {
            log.warn("Failed to get recent activity for project {}: {}", projectId, e.getMessage());
            return new ArrayList<>();
        }
    }

    private void updateProjectQualityScore(Long projectId) {
        try {
            BigDecimal averageScore = projectMapper.getProjectAverageQualityScore(projectId);
            if (averageScore != null) {
                projectMapper.updateProjectQualityScore(projectId, averageScore);
            }
        } catch (Exception e) {
            log.warn("Failed to update quality score for project {}: {}", projectId, e.getMessage());
        }
    }

//    private ProjectExportResponse generateProjectExport(Project project, List<ProjectPageExportData> pages, ExportFormat format) {
//        try {
//            ProjectExportResponse response = new ProjectExportResponse();
//            response.setProjectTitle(project.getTitle());
//            response.setExportFormat(format.name());
//            response.setExportedAt(LocalDateTime.now());
//
//            switch (format) {
//                case PDF:
//                    String pdfPath = fileUtil.generatePDFExport(project, pages, uploadDirectory);
//                    response.setDownloadUrl("/api/exports/download/" + pdfPath);
//                    response.setFileSize(fileUtil.getFileSize(pdfPath));
//                    break;
//
//                case EPUB:
//                    String epubPath = fileUtil.generateEPUBExport(project, pages, uploadDirectory);
//                    response.setDownloadUrl("/api/exports/download/" + epubPath);
//                    response.setFileSize(fileUtil.getFileSize(epubPath));
//                    break;
//
//                case HTML:
//                    String htmlPath = fileUtil.generateHTMLExport(project, pages, uploadDirectory);
//                    response.setDownloadUrl("/api/exports/download/" + htmlPath);
//                    response.setFileSize(fileUtil.getFileSize(htmlPath));
//                    break;
//
//                case DOCX:
//                    String docxPath = fileUtil.generateDOCXExport(project, pages, uploadDirectory);
//                    response.setDownloadUrl("/api/exports/download/" + docxPath);
//                    response.setFileSize(fileUtil.getFileSize(docxPath));
//                    break;
//
//                default:
//                    throw new IllegalArgumentException("Unsupported export format: " + format);
//            }
//
//            return response;
//        } catch (Exception e) {
//            log.error("Failed to generate export for project {}: {}", project.getId(), e.getMessage());
//            throw new RuntimeException("Export generation failed: " + e.getMessage(), e);
//        }
//    }

    private void logProjectActivity(Long projectId, Long userId, ProjectActivityType activityType, String description) {
        try {
            ProjectActivity activity = new ProjectActivity();
            activity.setProjectId(projectId);
            activity.setUserId(userId);
            activity.setActivityType(activityType);
            activity.setDescription(description);
            activity.setCreatedAt(LocalDateTime.now());

            projectMapper.insertProjectActivity(activity);
        } catch (Exception e) {
            log.warn("Failed to log project activity: {}", e.getMessage());
        }
    }

    private boolean isPageAlreadyAssignedForRole(Long projectId, Integer pageNumber, ProjectRole role) {
        try {
            List<ProjectMember> members = projectMemberMapper.getProjectMembersByProjectIdAndRole(projectId, String.valueOf(role));
            for (ProjectMember member : members) {
                if (member.getAssignedPages() != null && !member.getAssignedPages().isEmpty()) {
                    if (member.getAssignedPages().equals("ALL")) {
                        return true;
                    }

                    String assignedPagesStr = member.getAssignedPages();
                    if (assignedPagesStr.contains(pageNumber.toString())) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            log.warn("Error checking page assignment for page {} and role {}: {}",
                    pageNumber, role, e.getMessage());
            return false;
        }
    }

    private String validateRoleSpecificRequirements(ProjectRole role, JoinProjectRequest request, Project project) {
        switch (role) {
            case PROJECT_MANAGER:
                // Project managers typically need elevated permissions
                break;
            case TRANSLATOR:
                // Translators might need language proficiency validation
                break;
            case CONTENT_PROVIDER:
                // Content providers might need specific qualifications
                break;
            case EDITOR:
                // Editors might need editing experience or qualifications
                break;
            case PROOFREADER:
                // Editors and proofreaders might need language proficiency validation
                break;
            case ILLUSTRATOR:
                // Illustrators might need portfolio or skill validation
                break;
            case REVIEWER:
                // Reviewers might need experience validation
                break;
            case VOLUNTEER:
                // Volunteers might have minimal requirements
                break;
            default:
                return "Unknown role specified";
        }
        return null;
    }

    private void updatePageAssignments(Long projectId, Long userId, ProjectRole role, List<Integer> pageNumbers) {
        try {
            for (Integer pageNumber : pageNumbers) {
                switch (role) {
                    case TRANSLATOR:
                        projectPageMapper.assignTranslatorToPage(projectId, pageNumber, userId);
                        break;
                    case EDITOR:
                        projectPageMapper.assignEditorToPage(projectId, pageNumber, userId);
                        break;
                    case ILLUSTRATOR:
                        projectPageMapper.assignIllustratorToPage(projectId, pageNumber, userId);
                        break;
                    case PROOFREADER:
                        projectPageMapper.assignProofreaderToPage(projectId, pageNumber, userId);
                        break;
                    case REVIEWER:
                        projectPageMapper.assignReviewerToPage(projectId, pageNumber, userId);
                        break;
                    default:
                        break;
                }
            }
        } catch (Exception e) {
            log.error("Failed to update page assignments for user {} in project {}: {}",
                    userId, projectId, e.getMessage());
            throw new RuntimeException("Failed to update page assignments: " + e.getMessage(), e);
        }
    }

    private void updateProjectMemberCount(Long projectId) {
        try {
            int memberCount = projectMemberMapper.getActiveMemberCountByProjectId(projectId);
            projectMapper.updateMemberCount(projectId, memberCount);
        } catch (Exception e) {
            log.warn("Failed to update member count for project {}: {}", projectId, e.getMessage());
        }
    }

    private boolean isUserAssignedToPage(ProjectMember projectMember, Integer pageNumber) {
        String assignedPages = projectMember.getAssignedPages();

        if (assignedPages == null || assignedPages.isEmpty()) {
            return false;
        }

        if ("ALL".equals(assignedPages)) {
            return true;
        }

        try {
            String cleanedPages = assignedPages.replaceAll("[\\[\\]\\s]", "");
            String[] pageArray = cleanedPages.split(",");

            for (String page : pageArray) {
                if (page.trim().equals(pageNumber.toString())) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("Error parsing assigned pages '{}' for user {}: {}",
                    assignedPages, projectMember.getUserId(), e.getMessage());
        }

        return false;
    }

    private void updateProjectTranslationProgress(Long projectId) {
        try {
            int completedPages = projectPageMapper.getCompletedTranslationPagesCount(projectId);
            projectMapper.updateTranslationCompletedPages(projectId, completedPages);
        } catch (Exception e) {
            log.warn("Failed to update translation progress for project {}: {}", projectId, e.getMessage());
        }
    }

    private void updateProjectEditingProgress(Long projectId) {
        try {
            int completedPages = projectPageMapper.getCompletedEditingPagesCount(projectId);
            projectMapper.updateEditingCompletedPages(projectId, completedPages);
        } catch (Exception e) {
            log.warn("Failed to update editing progress for project {}: {}", projectId, e.getMessage());
        }
    }

    private void updateProjectIllustrationProgress(Long projectId) {
        try {
            int completedPages = projectPageMapper.getCompletedIllustrationPagesCount(projectId);
            projectMapper.updateIllustrationCompletedPages(projectId, completedPages);
        } catch (Exception e) {
            log.warn("Failed to update illustration progress for project {}: {}", projectId, e.getMessage());
        }
    }

    private void updateProjectProofreadingProgress(Long projectId) {
        try {
            int completedPages = projectPageMapper.getCompletedProofreadingPagesCount(projectId);
            projectMapper.updateProofreadingCompletedPages(projectId, completedPages);
        } catch (Exception e) {
            log.warn("Failed to update proofreading progress for project {}: {}", projectId, e.getMessage());
        }
    }

    private void updateProjectTranscriptionProgress(Long projectId) {
        try {
            int completedPages = projectPageMapper.getCompletedTranscriptionPagesCount(projectId);
            projectMapper.updateTranscriptionCompletedPages(projectId, completedPages);
        } catch (Exception e) {
            log.warn("Failed to update transcription progress for project {}: {}", projectId, e.getMessage());
        }
    }

    private void updateProjectOverallProgress(Long projectId) {
        try {
            Project project = projectMapper.getProjectByIdEntity(projectId);
            if (project != null && project.getTotalPages() > 0) {

                int totalPossibleTasks = project.getTotalPages() * 5; // 5 types of work per page
                int completedTasks = project.getTranscriptionCompletedPages() +
                        project.getTranslationCompletedPages() +
                        project.getEditingCompletedPages() +
                        project.getIllustrationCompletedPages() +
                        (project.getProofreadingCompletedPages() != null ? project.getProofreadingCompletedPages() : 0);

                BigDecimal overallProgress = BigDecimal.valueOf(completedTasks)
                        .divide(BigDecimal.valueOf(totalPossibleTasks), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));

                projectMapper.updateOverallProgress(projectId, overallProgress);

                // Check if project can be auto-completed
                if (overallProgress.compareTo(BigDecimal.valueOf(100)) == 0 &&
                        project.getStatus() == ProjectStatus.ACTIVE) {

                    // Auto-mark as ready for completion
                    Project updateProject = new Project();
                    updateProject.setId(projectId);
                    updateProject.setStatus(ProjectStatus.READY_FOR_COMPLETION);
                    updateProject.setUpdatedAt(LocalDateTime.now());
                    projectMapper.updateProject(updateProject);

                    // Notify project creator
                    createNotification(project.getCreatedBy(),
                            "Your project '" + project.getTitle() + "' is ready for completion!",
                            NotificationType.PROJECT_READY_FOR_COMPLETION, projectId, null);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to update overall progress for project {}: {}", projectId, e.getMessage());
        }
    }

    // Utility method to get current user ID (implement based on your security context)
    private Long getCurrentUserId() {
        // Implementation depends on your security configuration
        // This might use SecurityContextHolder.getContext().getAuthentication()
        // For now, returning a placeholder
        return 1L; // Replace with actual implementation
    }
}
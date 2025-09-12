package com.naskah.demo.service.impl;

import com.naskah.demo.mapper.*;
import com.naskah.demo.model.dto.response.*;
import com.naskah.demo.model.entity.ProjectFollow;
import com.naskah.demo.model.entity.ProjectReaction;
import com.naskah.demo.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final ProjectFollowMapper projectFollowMapper;
    private final NotificationMapper notificationMapper;
    private final ProjectReactionMapper projectReactionMapper;
    private final ProjectCommentMapper projectCommentMapper;
    private final ProjectPageMapper projectPageMapper;
    private final PageReactionMapper pageReactionMapper;
    private final PageCommentMapper pageCommentMapper;
    private static final String SUCCESS = "Success";


    @Override
    public DataResponse<UserDashboardResponse> getUserDashboard() {
        try {
            Long userId = getCurrentUserId();

            UserDashboardResponse dashboard = new UserDashboardResponse();
            dashboard.setUserId(userId);

            // Get user's projects (created by user)
            List<ProjectResponse> createdProjects = projectMapper.getProjectsByCreator(userId);
            for (ProjectResponse project : createdProjects) {
                enhanceProjectWithEngagementData(project, userId);
            }
            dashboard.setCreatedProjects(createdProjects);

            // Get user's joined projects
            List<ProjectMembershipResponse> joinedProjects = projectMemberMapper.getUserProjectMemberships(userId);
            dashboard.setJoinedProjects(joinedProjects);

            // Get user's followed projects
            List<ProjectResponse> followedProjects = projectFollowMapper.getUserFollowedProjects(userId);
            for (ProjectResponse project : followedProjects) {
                enhanceProjectWithEngagementData(project, userId);
            }
            dashboard.setFollowedProjects(followedProjects);

            // Get user's notifications
            List<NotificationResponse> notifications = notificationMapper.getUserNotifications(userId, 0, 20, "DESC");
            dashboard.setRecentNotifications(notifications);

            // Get user's contribution statistics
            UserContributionStats contributionStats = calculateUserContributionStats(userId);
            dashboard.setContributionStats(contributionStats);

            // Get user's recent activity
            List<UserActivityResponse> recentActivity = getUserRecentActivity(userId, 15);
            dashboard.setRecentActivity(recentActivity);

            return new DataResponse<>(SUCCESS, "Dashboard data fetched successfully",
                    HttpStatus.OK.value(), dashboard);

        } catch (Exception e) {
            log.error("Failed to get user dashboard for user {}: {}", getCurrentUserId(), e.getMessage(), e);
            throw e;
        }
    }

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

    private UserContributionStats calculateUserContributionStats(Long userId) {
        try {
            UserContributionStats stats = new UserContributionStats();

            // Get contribution counts by role
            stats.setPagesTranslated(projectPageMapper.getUserTranslationCount(userId));
            stats.setPagesEdited(projectPageMapper.getUserEditingCount(userId));
            stats.setPagesIllustrated(projectPageMapper.getUserIllustrationCount(userId));
            stats.setPagesProofread(projectPageMapper.getUserProofreadingCount(userId));
            stats.setPagesTranscribed(projectPageMapper.getUserTranscriptionCount(userId));
            stats.setPagesReviewed(projectPageMapper.getUserReviewCount(userId));

            // Calculate total contributions
            stats.setTotalContributions(
                    stats.getPagesTranslated() + stats.getPagesEdited() + stats.getPagesIllustrated() +
                            stats.getPagesProofread() + stats.getPagesTranscribed() + stats.getPagesReviewed());

            // Get project counts
            stats.setProjectsCreated(projectMapper.getUserCreatedProjectCount(userId));
            stats.setProjectsJoined(projectMemberMapper.getUserJoinedProjectCount(userId));
            stats.setProjectsFollowed(projectFollowMapper.getUserFollowedProjectCount(userId));

            // Get quality metrics
            stats.setAverageQualityScore(projectMapper.getUserAverageQualityScore(userId));
            stats.setTotalReactionsReceived(calculateUserTotalReactionsReceived(userId));
            stats.setTotalCommentsReceived(calculateUserTotalCommentsReceived(userId));

            return stats;
        } catch (Exception e) {
            log.warn("Failed to calculate contribution stats for user {}: {}", userId, e.getMessage());
            return new UserContributionStats();
        }
    }


    private Long calculateUserTotalReactionsReceived(Long userId) {
        try {
            Long projectReactions = projectReactionMapper.getUserTotalProjectReactionsReceived(userId);
            Long pageReactions = pageReactionMapper.getUserTotalPageReactionsReceived(userId);
            return (projectReactions != null ? projectReactions : 0) + (pageReactions != null ? pageReactions : 0);
        } catch (Exception e) {
            log.warn("Failed to calculate total reactions for user {}: {}", userId, e.getMessage());
            return 0L;
        }
    }

    private Long calculateUserTotalCommentsReceived(Long userId) {
        try {
            Long projectComments = projectCommentMapper.getUserTotalProjectCommentsReceived(userId);
            Long pageComments = pageCommentMapper.getUserTotalPageCommentsReceived(userId);
            return (projectComments != null ? projectComments : 0) + (pageComments != null ? pageComments : 0);
        } catch (Exception e) {
            log.warn("Failed to calculate total comments for user {}: {}", userId, e.getMessage());
            return 0L;
        }
    }

    private List<UserActivityResponse> getUserRecentActivity(Long userId, int limit) {
        try {
            return projectMapper.getUserRecentActivity(userId, limit);
        } catch (Exception e) {
            log.warn("Failed to get recent activity for user {}: {}", userId, e.getMessage());
            return new ArrayList<>();
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

//package com.naskah.demo.controller;
//
//import lombok.RequiredArgsConstructor;
//import org.skah.digitallibrary.dto.ApiResponse;
//import org.skah.digitallibrary.dto.PageSubmissionDto;
//import org.skah.digitallibrary.model.ProjectPage;
//import org.skah.digitallibrary.model.User;
//import org.skah.digitallibrary.service.ProjectPageService;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.web.bind.annotation.*;
//import javax.validation.Valid;
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/pages")
//@RequiredArgsConstructor
//public class ProjectPageController {
//
//    private final ProjectPageService projectPageService;
//
//    @GetMapping("/available")
//    public ResponseEntity<ApiResponse<List<ProjectPage>>> getAvailablePages(
//            @AuthenticationPrincipal User currentUser) {
//        List<ProjectPage> pages = projectPageService.findAvailableForUser(currentUser);
//        return ResponseEntity.ok(ApiResponse.success(pages, "Available pages retrieved successfully"));
//    }
//
//    @GetMapping("/my-pages")
//    public ResponseEntity<ApiResponse<List<ProjectPage>>> getMyPages(
//            @AuthenticationPrincipal User currentUser) {
//        List<ProjectPage> pages = projectPageService.findCheckedOutByUser(currentUser.getId());
//        return ResponseEntity.ok(ApiResponse.success(pages, "User pages retrieved successfully"));
//    }
//
//    @GetMapping("/{id}")
//    public ResponseEntity<ApiResponse<ProjectPage>> getPageById(@PathVariable Long id) {
//        ProjectPage page = projectPageService.findById(id)
//                .orElseThrow(() -> new RuntimeException("Page not found"));
//        return ResponseEntity.ok(ApiResponse.success(page, "Page retrieved successfully"));
//    }
//
//    @PostMapping("/{id}/checkout")
//    public ResponseEntity<ApiResponse<ProjectPage>> checkoutPage(
//            @PathVariable Long id,
//            @AuthenticationPrincipal User currentUser) {
//        try {
//            ProjectPage page = projectPageService.checkoutPage(id, currentUser.getId());
//            return ResponseEntity.ok(ApiResponse.success(page, "Page checked out successfully"));
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
//        }
//    }
//
//    @PostMapping("/{id}/submit")
//    public ResponseEntity<ApiResponse<ProjectPage>> submitPage(
//            @PathVariable Long id,
//            @Valid @RequestBody PageSubmissionDto submissionDto,
//            @AuthenticationPrincipal User currentUser) {
//        try {
//            ProjectPage page = projectPageService.submitPage(id, submissionDto.getText(), submissionDto.getNotes());
//            return ResponseEntity.ok(ApiResponse.success(page, "Page submitted successfully"));
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
//        }
//    }
//
//    @PostMapping("/{id}/release")
//    public ResponseEntity<ApiResponse<String>> releasePage(@PathVariable Long id) {
//        try {
//            projectPageService.releasePage(id);
//            return ResponseEntity.ok(ApiResponse.success("Page released successfully", null));
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
//        }
//    }
//
//    @GetMapping("/project/{projectId}")
//    public ResponseEntity<ApiResponse<List<ProjectPage>>> getProjectPages(@PathVariable Long projectId) {
//        List<ProjectPage> pages = projectPageService.findByProject(projectId);
//        return ResponseEntity.ok(ApiResponse.success(pages, "Project pages retrieved successfully"));
//    }
//}

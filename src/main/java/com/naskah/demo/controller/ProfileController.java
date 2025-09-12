//package com.naskah.demo.controller;
//
//import com.naskah.demo.model.dto.request.ChangePasswordRequest;
//import com.naskah.demo.model.dto.response.DataResponse;
//import jakarta.validation.Valid;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//
//public class ProfileController {
//
//    @GetMapping("/profile")
//    public ResponseEntity<DataResponse<UserProfileResponse>> getUserProfile() {
//        String username = headerHolder.getUsername();
//        DataResponse<UserProfileResponse> response = authService.getUserProfile(username);
//        return ResponseEntity.ok(response);
//    }
//
//    @PutMapping("/profile")
//    public ResponseEntity<DataResponse<String>> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
//        String username = headerHolder.getUsername();
//        DataResponse<String> response = authService.updateProfile(username, request);
//        return ResponseEntity.ok(response);
//    }
//
//    @PostMapping(value = "/upload-avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    public ResponseEntity<DataResponse<String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
//        String username = headerHolder.getUsername();
//        DataResponse<String> response = authService.uploadAvatar(username, file);
//        return ResponseEntity.ok(response);
//    }
//
//    @PostMapping("/change-password")
//    public ResponseEntity<DataResponse<String>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
//        String username = headerHolder.getUsername();
//        DataResponse<String> response = authService.changePassword(username, request);
//        return ResponseEntity.ok(response);
//    }
//
//    @GetMapping("/contributions")
//    public ResponseEntity<DataResponse<UserContributionResponse>> getUserContributions() {
//        String username = headerHolder.getUsername();
//        DataResponse<UserContributionResponse> response = authService.getUserContributions(username);
//        return ResponseEntity.ok(response);
//    }
//}

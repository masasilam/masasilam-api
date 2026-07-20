package com.masasilam.app.controller;

import com.masasilam.app.model.dto.request.ChangePasswordRequest;
import com.masasilam.app.model.dto.request.UpdateUserRequest;
import com.masasilam.app.model.dto.response.DataResponse;
import com.masasilam.app.model.dto.response.UserResponse;
import com.masasilam.app.service.common.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    public ResponseEntity<DataResponse<List<UserResponse>>> getAllUsers() {
        DataResponse<List<UserResponse>> response = userService.getAllUsers();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<DataResponse<UserResponse>> getUserById(@PathVariable Long userId) {
        DataResponse<UserResponse> response = userService.getUserById(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<DataResponse<UserResponse>> updateUser(@PathVariable Long userId,
                                                                 @Valid @RequestBody UpdateUserRequest request) {
        DataResponse<UserResponse> response = userService.updateUser(userId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{userId}/change-password")
    public ResponseEntity<DataResponse<String>> changePassword(@PathVariable Long userId,
                                                               @Valid @RequestBody ChangePasswordRequest request) {
        DataResponse<String> response = userService.changePassword(userId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<DataResponse<String>> deleteUser(@PathVariable Long userId,
                                                           @RequestParam(defaultValue = "false") Boolean hardDelete) {
        DataResponse<String> response = userService.deleteUser(userId, hardDelete);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/profile-picture")
    public ResponseEntity<DataResponse<Map<String, String>>> uploadProfilePicture(@RequestParam("file") MultipartFile file,
                                                                                  @RequestParam("userId") Long userId) {
        DataResponse<Map<String, String>> response = userService.uploadProfilePicture(file, userId);
        return ResponseEntity.ok(response);
    }
}
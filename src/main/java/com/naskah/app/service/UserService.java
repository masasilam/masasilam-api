package com.naskah.app.service;

import com.naskah.app.model.dto.request.ChangePasswordRequest;
import com.naskah.app.model.dto.request.UpdateUserRequest;
import com.naskah.app.model.dto.response.DataResponse;
import com.naskah.app.model.dto.response.UserResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface UserService {
    DataResponse<List<UserResponse>> getAllUsers();
    DataResponse<UserResponse> getUserById(Long userId);
    DataResponse<UserResponse> updateUser(Long userId, UpdateUserRequest request);
    DataResponse<String> changePassword(Long userId, ChangePasswordRequest request);
    DataResponse<String> deleteUser(Long userId, Boolean hardDelete);
    DataResponse<Map<String, String>> uploadProfilePicture(MultipartFile file, Long userId);
}
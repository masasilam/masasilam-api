package com.naskah.demo.service.impl;

import com.naskah.demo.exception.custom.BadRequestException;
import com.naskah.demo.exception.custom.DataNotFoundException;
import com.naskah.demo.exception.custom.UnauthorizedException;
import com.naskah.demo.mapper.UserMapper;
import com.naskah.demo.model.dto.request.ChangePasswordRequest;
import com.naskah.demo.model.dto.request.UpdateUserRequest;
import com.naskah.demo.model.dto.response.DataResponse;
import com.naskah.demo.model.dto.response.UserResponse;
import com.naskah.demo.model.entity.User;
import com.naskah.demo.service.UserService;
import com.naskah.demo.util.file.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final FileUtil fileUtil;  // ✅ INJECT FileUtil

    @Override
    public DataResponse<List<UserResponse>> getAllUsers() {
        log.info("Fetching all users");
        List<UserResponse> users = userMapper.findAllUsers();

        log.info("Successfully fetched {} users", users.size());
        return new DataResponse<>("success", "Users retrieved successfully", HttpStatus.OK.value(), users);
    }

    @Override
    public DataResponse<UserResponse> getUserById(Long userId) {
        log.info("Fetching user with ID: {}", userId);

        User user = userMapper.findUserById(userId);
        if (user == null || !user.getIsActive()) {
            throw new DataNotFoundException();
        }

        UserResponse response = mapToUserResponse(user);

        log.info("Successfully fetched user: {}", user.getUsername());
        return new DataResponse<>("success", "User retrieved successfully", HttpStatus.OK.value(), response);
    }

    @Override
    @Transactional
    public DataResponse<UserResponse> updateUser(Long userId, UpdateUserRequest request) {
        log.info("Updating user with ID: {}", userId);

        User user = userMapper.findUserById(userId);
        if (user == null || !user.getIsActive()) {
            throw new DataNotFoundException();
        }

        // Update user fields
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        if (request.getProfilePictureUrl() != null) {
            user.setProfilePictureUrl(request.getProfilePictureUrl());
        }
        if (request.getEmailNotifications() != null) {
            user.setEmailNotifications(request.getEmailNotifications());
        }

        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateUser(user);

        UserResponse response = mapToUserResponse(user);

        log.info("Successfully updated user: {}", user.getUsername());
        return new DataResponse<>("success", "User updated successfully", HttpStatus.OK.value(), response);
    }

    @Override
    @Transactional
    public DataResponse<String> changePassword(Long userId, ChangePasswordRequest request) {
        log.info("Changing password for user ID: {}", userId);

        User user = userMapper.findUserById(userId);
        if (user == null || !user.getIsActive()) {
            throw new DataNotFoundException();
        }

        // Verify current password
        if (user.getPasswordHash() != null && !passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException();
        }

        // Update password
        String hashedPassword = passwordEncoder.encode(request.getNewPassword());
        userMapper.updateUserPassword(userId, hashedPassword);

        log.info("Successfully changed password for user: {}", user.getUsername());
        return new DataResponse<>("success", "Password changed successfully", HttpStatus.OK.value(), null);
    }

    @Override
    @Transactional
    public DataResponse<String> deleteUser(Long userId, Boolean hardDelete) {
        log.info("Deleting user with ID: {} (hard delete: {})", userId, hardDelete);

        User user = userMapper.findUserById(userId);
        if (user == null) {
            throw new DataNotFoundException();
        }

        if (Boolean.TRUE.equals(hardDelete)) {
            // Hard delete - remove user and related data
            userMapper.deleteUserRoles(userId);
            userMapper.deleteVerificationToken(userId);
            userMapper.deletePasswordResetToken(userId);
            userMapper.hardDeleteUser(userId);

            log.info("Successfully hard deleted user: {}", user.getUsername());
            return new DataResponse<>("success", "User permanently deleted", HttpStatus.OK.value(), null);
        } else {
            // Soft delete - mark as inactive
            userMapper.softDeleteUser(userId);

            log.info("Successfully soft deleted user: {}", user.getUsername());
            return new DataResponse<>("success", "User deactivated successfully", HttpStatus.OK.value(), null);
        }
    }

    // ✅ TAMBAHKAN METHOD UPLOAD PROFILE PICTURE
    @Override
    @Transactional
    public DataResponse<Map<String, String>> uploadProfilePicture(MultipartFile file, Long userId) {
        log.info("Uploading profile picture for user ID: {}", userId);

        // Validate user exists
        User user = userMapper.findUserById(userId);
        if (user == null || !user.getIsActive()) {
            throw new DataNotFoundException();
        }

        // Validate file
        if (file == null || file.isEmpty()) {
            throw new BadRequestException();
        }

        // Validate file type
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.matches(".*\\.(jpg|jpeg|png|gif)$")) {
            throw new BadRequestException();
        }

        // Validate file size (max 2MB)
        if (file.getSize() > 2 * 1024 * 1024) {
            throw new BadRequestException();
        }

        try {
            // Delete old profile picture if exists
            if (user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().isEmpty()) {
                fileUtil.deleteFile(user.getProfilePictureUrl());
            }

            // Upload new profile picture
            String imageUrl = fileUtil.uploadAuthorPhoto(file, user.getUsername() + "-" + userId);

            // Update user profile picture URL
            user.setProfilePictureUrl(imageUrl);
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.updateUser(user);

            Map<String, String> result = new HashMap<>();
            result.put("url", imageUrl);

            log.info("Successfully uploaded profile picture for user: {}", user.getUsername());
            return new DataResponse<>("success", "Profile picture uploaded successfully", HttpStatus.OK.value(), result);

        } catch (IOException e) {
            log.error("Failed to upload profile picture", e);
            throw new BadRequestException();
        }
    }

    private UserResponse mapToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setFullName(user.getFullName());
        response.setProfilePictureUrl(user.getProfilePictureUrl());
        response.setBio(user.getBio());
        response.setTotalBooksRead(user.getTotalBooksRead());
        response.setReadingStreakDays(user.getReadingStreakDays());
        response.setContributedBooksCount(user.getContributedBooksCount());
        response.setAverageRating(user.getAverageRating());
        response.setExperiencePoints(user.getExperiencePoints());
        response.setLevel(user.getLevel());
        return response;
    }
}
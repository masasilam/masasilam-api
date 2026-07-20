package com.masasilam.app.service.common.impl;

import com.masasilam.app.exception.custom.BadRequestException;
import com.masasilam.app.exception.custom.DataNotFoundException;
import com.masasilam.app.exception.custom.UnauthorizedException;
import com.masasilam.app.mapper.UserMapper;
import com.masasilam.app.model.dto.request.ChangePasswordRequest;
import com.masasilam.app.model.dto.request.UpdateUserRequest;
import com.masasilam.app.model.dto.response.DataResponse;
import com.masasilam.app.model.dto.response.UserResponse;
import com.masasilam.app.model.entity.User;
import com.masasilam.app.service.common.UserService;
import com.masasilam.app.util.file.FileUtil;
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
    private final FileUtil fileUtil;

    private static final String SUCCESS = "success";

    @Override
    public DataResponse<List<UserResponse>> getAllUsers() {
        log.info("Fetching all users");
        List<UserResponse> users = userMapper.findAllUsers();

        log.info("Successfully fetched {} users", users.size());
        return new DataResponse<>(SUCCESS, "Users retrieved successfully", HttpStatus.OK.value(), users);
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
        return new DataResponse<>(SUCCESS, "User retrieved successfully", HttpStatus.OK.value(), response);
    }

    @Override
    @Transactional
    public DataResponse<UserResponse> updateUser(Long userId, UpdateUserRequest request) {
        User user = userMapper.findUserById(userId);
        if (user == null || !user.getIsActive()) {
            throw new DataNotFoundException();
        }

        if (request.getUsername() != null) {
            User existingUser = userMapper.findUserByUsername(request.getUsername());
            if (existingUser != null && !existingUser.getId().equals(userId)) {
                throw new BadRequestException();
            }
            user.setUsername(request.getUsername());
        }

        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getBio() != null) user.setBio(request.getBio());
        if (request.getProfilePictureUrl() != null) user.setProfilePictureUrl(request.getProfilePictureUrl());
        if (request.getEmailNotifications() != null) user.setEmailNotifications(request.getEmailNotifications());

        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateUser(user);

        return new DataResponse<>(SUCCESS, "User updated successfully", HttpStatus.OK.value(), mapToUserResponse(user));
    }

    @Override
    @Transactional
    public DataResponse<String> changePassword(Long userId, ChangePasswordRequest request) {
        log.info("Changing password for user ID: {}", userId);

        User user = userMapper.findUserById(userId);
        if (user == null || !user.getIsActive()) {
            throw new DataNotFoundException();
        }

        if (user.getPasswordHash() != null && !passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException();
        }

        String hashedPassword = passwordEncoder.encode(request.getNewPassword());
        userMapper.updateUserPassword(userId, hashedPassword);

        log.info("Successfully changed password for user: {}", user.getUsername());
        return new DataResponse<>(SUCCESS, "Password changed successfully", HttpStatus.OK.value(), null);
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
            userMapper.deleteUserRoles(userId);
            userMapper.deleteVerificationToken(userId);
            userMapper.deletePasswordResetToken(userId);
            userMapper.hardDeleteUser(userId);

            log.info("Successfully hard deleted user: {}", user.getUsername());
            return new DataResponse<>(SUCCESS, "User permanently deleted", HttpStatus.OK.value(), null);
        } else {
            userMapper.softDeleteUser(userId);

            log.info("Successfully soft deleted user: {}", user.getUsername());
            return new DataResponse<>(SUCCESS, "User deactivated successfully", HttpStatus.OK.value(), null);
        }
    }

    @Override
    @Transactional
    public DataResponse<Map<String, String>> uploadProfilePicture(MultipartFile file, Long userId) {
        log.info("Uploading profile picture for user ID: {}", userId);

        User user = userMapper.findUserById(userId);
        if (user == null || !user.getIsActive()) {
            throw new DataNotFoundException();
        }

        if (file == null || file.isEmpty()) {
            throw new BadRequestException();
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.matches(".*\\.(jpg|jpeg|png|gif)$")) {
            throw new BadRequestException();
        }

        if (file.getSize() > 2 * 1024 * 1024) {
            throw new BadRequestException();
        }

        try {
            if (user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().isEmpty()) {
                fileUtil.deleteFile(user.getProfilePictureUrl());
            }

            String imageUrl = fileUtil.uploadAuthorPhoto(file, user.getUsername() + "-" + userId);

            user.setProfilePictureUrl(imageUrl);
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.updateUser(user);

            Map<String, String> result = new HashMap<>();
            result.put("url", imageUrl);

            log.info("Successfully uploaded profile picture for user: {}", user.getUsername());
            return new DataResponse<>(SUCCESS, "Profile picture uploaded successfully", HttpStatus.OK.value(), result);

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
//package com.naskah.demo.service.impl;
//
//import com.naskah.demo.exception.custom.BadRequestException;
//import com.naskah.demo.exception.custom.DataNotFoundException;
//import com.naskah.demo.model.dto.request.ChangePasswordRequest;
//import com.naskah.demo.model.dto.response.DataResponse;
//import com.naskah.demo.model.entity.User;
//import lombok.AllArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.time.LocalDateTime;
//
//@Service
//@Slf4j
//@AllArgsConstructor
//public class ProfileServiceImpl implements  ProfileService{
//
//    @Override
//    public DataResponse<UserProfileResponse> getUserProfile(String username) {
//        try {
//            User user = userMapper.findUserByUsername(username);
//            if (user == null) {
//                throw new DataNotFoundException();
//            }
//
//            UserStats stats = userMapper.getUserStats(user.getId());
//
//            UserProfileResponse profile = new UserProfileResponse();
//            profile.setUsername(user.getUsername());
//            profile.setEmail(user.getEmail());
//            profile.setFullName(user.getFullName());
//            profile.setPhoneNumber(user.getPhoneNumber());
//            profile.setBio(user.getBio());
//            profile.setProfession(user.getProfession());
//            profile.setInstitution(user.getInstitution());
//            profile.setCountry(user.getCountry());
//            profile.setPreferredLanguages(user.getPreferredLanguages());
//            profile.setInterests(user.getInterests());
//            profile.setMembershipType(user.getMembershipType());
//            profile.setAvatarUrl(user.getAvatarUrl());
//            profile.setEmailVerified(user.getEmailVerified());
//            profile.setJoinedAt(user.getCreatedAt());
//            profile.setLastLoginAt(user.getLastLoginAt());
//            profile.setStats(stats);
//
//            return new DataResponse<>("Success", "Profile retrieved successfully", HttpStatus.OK.value(), profile);
//        } catch (Exception e) {
//            log.error("Error retrieving user profile", e);
//            throw new RuntimeException("Failed to retrieve profile");
//        }
//    }
//
//    @Override
//    public DataResponse<String> updateProfile(String username, UpdateProfileRequest request) {
//        try {
//            User user = userMapper.findUserByUsername(username);
//            if (user == null) {
//                throw new NotFoundException("User not found");
//            }
//
//            user.setFullName(request.getFullName());
//            user.setPhoneNumber(request.getPhoneNumber());
//            user.setBio(request.getBio());
//            user.setProfession(request.getProfession());
//            user.setInstitution(request.getInstitution());
//            user.setCountry(request.getCountry());
//            user.setPreferredLanguages(request.getPreferredLanguages());
//            user.setInterests(request.getInterests());
//            user.setUpdatedAt(LocalDateTime.now());
//
//            userMapper.updateUserProfile(user);
//
//            return new DataResponse<>("Success", "Profile updated successfully", HttpStatus.OK.value(), null);
//        } catch (Exception e) {
//            log.error("Error updating user profile", e);
//            throw new RuntimeException("Failed to update profile");
//        }
//    }
//
//    @Override
//    public DataResponse<String> uploadAvatar(String username, MultipartFile file) {
//        try {
//            User user = userMapper.findUserByUsername(username);
//            if (user == null) {
//                throw new DataNotFoundException();
//            }
//
//            String avatarUrl = fileStorageService.uploadAvatar(file, username);
//            userMapper.updateUserAvatar(user.getId(), avatarUrl);
//
//            return new DataResponse<>("Success", "Avatar uploaded successfully", HttpStatus.OK.value(), avatarUrl);
//        } catch (Exception e) {
//            log.error("Error uploading avatar", e);
//            throw new RuntimeException("Failed to upload avatar");
//        }
//    }
//
//    @Override
//    public DataResponse<String> changePassword(String username, ChangePasswordRequest request) {
//        try {
//            User user = userMapper.findUserByUsername(username);
//            if (user == null) {
//                throw new DataNotFoundException();
//            }
//
//            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
//                throw new BadRequestException();
//            }
//
//            String hashedPassword = passwordEncoder.encode(request.getNewPassword());
//            userMapper.updateUserPassword(user.getId(), hashedPassword);
//
//            return new DataResponse<>("Success", "Password changed successfully", HttpStatus.OK.value(), null);
//        } catch (Exception e) {
//            log.error("Error changing password", e);
//            throw new RuntimeException("Failed to change password");
//        }
//    }
//}

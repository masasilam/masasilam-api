package com.naskah.demo.mapper;

import com.naskah.demo.model.dto.response.UserResponse;
import com.naskah.demo.model.entity.Role;
import com.naskah.demo.model.entity.User;
import com.naskah.demo.model.entity.UserActivity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserMapper {
    User findUserByUsername(@Param("username") String username);
    void insertUserActivity(UserActivity userActivity);
    List<Role> findUserRoles(@Param("userId") Long userId);
    void insertUser(User user);
    void updateLastLogin(@Param("userId") Long userId, @Param("lastLoginAt") LocalDateTime lastLoginAt);
    User findUserByEmail(@Param("email") String email);
    void savePasswordResetToken(@Param("id") Long id, @Param("token") String token, @Param("expiresAt") LocalDateTime expiresAt);
    User findUserById(@Param("id") Long id);
    void updateUserPassword(@Param("id") Long id, @Param("passwordHash") String passwordHash);
    void deletePasswordResetToken(@Param("id") Long id);
    Role findRoleByName(@Param("roleName") String roleName);
    void assignRoleToUser(@Param("userId") Long userId, @Param("roleId") Long roleId);
    void saveVerificationToken(@Param("userId") Long userId, @Param("token") String token, @Param("expiresAt") LocalDateTime expiresAt);
    void linkGoogleAccount(@Param("userId") Long userId, @Param("googleId") String googleId);
    void verifyUserEmail(@Param("userId") Long userId);
    void deleteVerificationToken(@Param("userId") Long userId);
    List<UserResponse> findAllUsers();
}

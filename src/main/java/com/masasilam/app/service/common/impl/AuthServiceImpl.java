package com.masasilam.app.service.common.impl;

import com.masasilam.app.exception.custom.*;
import com.masasilam.app.mapper.UserMapper;
import com.masasilam.app.model.dto.request.*;
import com.masasilam.app.model.dto.response.*;
import com.masasilam.app.model.entity.*;
import com.masasilam.app.service.common.AuthService;
import com.masasilam.app.service.common.EmailService;
import com.masasilam.app.util.interceptor.JwtUtil;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private static final String DEFAULT_TOKEN_PREFIX = "Bearer ";
    private static final String READER = "READER";
    private static final String EMAIL_VERIFICATION = "EMAIL_VERIFICATION";
    private static final String REFRESH = "REFRESH";
    private static final String ACTIVE = "ACTIVE";
    private static final String BEGINNER = "BEGINNER";

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    @Value("${google.oauth.client-id}")
    private String googleClientId;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    public DataResponse<LoginResponse> login(LoginRequest request) {
        User user = userMapper.findUserByUsername(request.getUsername());
        if (Boolean.TRUE.equals(user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash()) ||
                !user.getIsActive()) || Boolean.TRUE.equals(!user.getEmailVerified())) {
            throw new UnauthorizedException();
        }

        userMapper.updateLastLogin(user.getId(), LocalDateTime.now());

        List<Role> userRoles = userMapper.findUserRoles(user.getId());
        List<String> roleNames = userRoles.stream()
                .map(Role::getName)
                .collect(Collectors.toList());

        String token = jwtUtil.generateToken(user.getUsername(), user.getFullName(), roleNames);
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setId(user.getId());
        loginResponse.setToken(token);
        loginResponse.setRefreshToken(refreshToken);
        loginResponse.setTokenType("Bearer");
        loginResponse.setUsername(user.getUsername());
        loginResponse.setName(user.getFullName());
        loginResponse.setEmail(user.getEmail());
        loginResponse.setRoles(roleNames.toArray(new String[0]));
        loginResponse.setExpiresIn(jwtExpiration);
        loginResponse.setFullName(user.getFullName());
        loginResponse.setProfilePictureUrl(user.getProfilePictureUrl());
        loginResponse.setBio(user.getBio());
        loginResponse.setEmailNotifications(user.getEmailNotifications());
        loginResponse.setLevel(user.getLevel());
        loginResponse.setTotalBooksRead(user.getTotalBooksRead());
        loginResponse.setReadingStreakDays(user.getReadingStreakDays());
        loginResponse.setContributedBooksCount(user.getContributedBooksCount());
        loginResponse.setAverageRating(user.getAverageRating());
        loginResponse.setExperiencePoints(user.getExperiencePoints());

        return new DataResponse<>("Success", "Login successful", HttpStatus.OK.value(), loginResponse);
    }

    @Override
    public DataResponse<String> logout(String token) {
        token = JwtUtil.extractAuthToken(token, DEFAULT_TOKEN_PREFIX);

        if (jwtUtil.validateToken(token)) {
            jwtUtil.blacklistToken(token);
            return new DataResponse<>("Success", "Logged out successfully", HttpStatus.OK.value(), null);
        }

        throw new UnauthorizedException();
    }

    @Override
    @Transactional
    public DataResponse<RegisterResponse> register(RegisterRequest request) {
        if (userMapper.findUserByUsername(request.getUsername()) != null || userMapper.findUserByEmail(request.getEmail()) != null) {
            throw new DataAlreadyExistsException();
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setProfilePictureUrl(null);
        user.setBio(request.getBio());
        user.setTotalBooksRead(0);
        user.setReadingStreakDays(0);
        user.setIsActive(true);
        user.setEmailVerified(false);
        user.setStatus(ACTIVE);
        user.setContributedBooksCount(0);
        user.setAverageRating(0.0);
        user.setExperiencePoints(0);
        user.setEmailNotifications(true);
        user.setLevel(BEGINNER);
        user.setGoogleId(null);
        user.setLastActiveDate(LocalDateTime.now());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setLastLoginAt(LocalDateTime.now());

        userMapper.insertUser(user);

        Role defaultRole = userMapper.findRoleByName(READER);
        if (defaultRole != null) {
            userMapper.assignRoleToUser(user.getId(), defaultRole.getId());
        }

        String verificationToken = jwtUtil.generateVerificationToken(user.getId());
        userMapper.saveVerificationToken(user.getId(), verificationToken, LocalDateTime.now().plusHours(24));

        emailService.sendVerificationEmail(user.getEmail(), user.getFullName(), frontendUrl + "/verifikasi-email?token=" + verificationToken);

        RegisterResponse response = new RegisterResponse();
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setEmailVerified(false);
        response.setRegisteredAt(user.getCreatedAt());
        response.setMessage("Registration successful. Please check your email to verify your account.");

        return new DataResponse<>("Success", "Registration successful", HttpStatus.CREATED.value(), response);
    }

    @Override
    @Transactional
    public DataResponse<LoginResponse> googleAuth(GoogleAuthRequest request) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(request.getIdToken());
            if (idToken == null) {
                throw new UnauthorizedException();
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String googleId = payload.getSubject();

            User user = userMapper.findUserByEmail(email);

            if (user == null) {
                user = new User();
                user.setUsername(email.split("@")[0] + "_" + System.currentTimeMillis());
                user.setEmail(email);
                user.setFullName(name);
                user.setGoogleId(googleId);
                user.setIsActive(true);
                user.setEmailVerified(true);
                user.setCreatedAt(LocalDateTime.now());
                user.setUpdatedAt(LocalDateTime.now());
                user.setProfilePictureUrl(null);
                user.setBio(null);
                user.setTotalBooksRead(0);
                user.setReadingStreakDays(0);
                user.setStatus(ACTIVE);
                user.setContributedBooksCount(0);
                user.setAverageRating(0.0);
                user.setExperiencePoints(0);
                user.setEmailNotifications(true);
                user.setLevel(BEGINNER);
                user.setLastActiveDate(LocalDateTime.now());
                user.setLastLoginAt(LocalDateTime.now());

                userMapper.insertUser(user);

                Role defaultRole = userMapper.findRoleByName(READER);
                if (defaultRole != null) {
                    userMapper.assignRoleToUser(user.getId(), defaultRole.getId());
                }
            } else if (user.getGoogleId() == null) {
                userMapper.linkGoogleAccount(user.getId(), googleId);
            }

            userMapper.updateLastLogin(user.getId(), LocalDateTime.now());

            List<Role> userRoles = userMapper.findUserRoles(user.getId());
            List<String> roleNames = userRoles.stream()
                    .map(Role::getName)
                    .collect(Collectors.toList());

            String token = jwtUtil.generateToken(user.getUsername(), user.getFullName(), roleNames);
            String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

            LoginResponse loginResponse = new LoginResponse();
            loginResponse.setId(user.getId());
            loginResponse.setToken(token);
            loginResponse.setRefreshToken(refreshToken);
            loginResponse.setTokenType("Bearer");
            loginResponse.setUsername(user.getUsername());
            loginResponse.setName(user.getFullName());
            loginResponse.setEmail(user.getEmail());
            loginResponse.setRoles(roleNames.toArray(new String[0]));
            loginResponse.setExpiresIn(jwtExpiration);
            loginResponse.setFullName(user.getFullName());
            loginResponse.setProfilePictureUrl(user.getProfilePictureUrl());
            loginResponse.setBio(user.getBio());
            loginResponse.setEmailNotifications(user.getEmailNotifications());
            loginResponse.setLevel(user.getLevel());
            loginResponse.setTotalBooksRead(user.getTotalBooksRead());
            loginResponse.setReadingStreakDays(user.getReadingStreakDays());
            loginResponse.setContributedBooksCount(user.getContributedBooksCount());
            loginResponse.setAverageRating(user.getAverageRating());
            loginResponse.setExperiencePoints(user.getExperiencePoints());

            return new DataResponse<>("Success", "Google authentication successful", HttpStatus.OK.value(), loginResponse);

        } catch (GeneralSecurityException | IOException e) {
            log.error("Google authentication failed", e);
            throw new ServiceUnavailableException();
        }
    }

    @Override
    public DataResponse<String> verifyEmail(String token) {
        log.info("Starting email verification for token: {}", token.substring(0, Math.min(token.length(), 20)) + "...");

        if (!jwtUtil.validateToken(token)) {
            log.error("Token validation failed");
            throw new BadRequestException();
        }
        log.info("Token validation passed");

        String tokenType = jwtUtil.extractTokenType(token);
        log.info("Extracted token type: {}", tokenType);
        if (!EMAIL_VERIFICATION.equals(tokenType)) {
            log.error("Invalid token type. Expected: EMAIL_VERIFICATION, Got: {}", tokenType);
            throw new BadRequestException();
        }

        Long userId = Long.valueOf(jwtUtil.extractUsername(token));
        log.info("Extracted user ID: {}", userId);

        User user = userMapper.findUserById(userId);
        if (user == null) {
            log.error("User not found for ID: {}", userId);
            throw new DataNotFoundException();
        }
        log.info("User found: {}", user.getEmail());

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            log.warn("Email already verified for user: {}", userId);
            throw new BadRequestException();
        }

        userMapper.verifyUserEmail(userId);
        userMapper.deleteVerificationToken(userId);
        return new DataResponse<>("Success", "Email verified successfully", HttpStatus.OK.value(), null);
    }

    @Override
    public DataResponse<String> resendVerificationEmail(String email) {
        User user = userMapper.findUserByEmail(email);
        if (user == null) {
            throw new DataNotFoundException();
        }

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BadRequestException();
        }

        String verificationToken = jwtUtil.generateVerificationToken(user.getId());
        userMapper.saveVerificationToken(user.getId(), verificationToken, LocalDateTime.now().plusHours(24));

        emailService.sendVerificationEmail(user.getEmail(), user.getFullName(),
                frontendUrl + "/verifikasi-email?token=" + verificationToken);

        return new DataResponse<>("Success", "Verification email sent", HttpStatus.OK.value(), null);
    }

    @Override
    public DataResponse<String> forgotPassword(ForgotPasswordRequest request) {
        User user = userMapper.findUserByEmail(request.getEmail());
        if (user == null) {
            return new DataResponse<>("Success", "If an account with this email exists, you will receive a password reset link", HttpStatus.OK.value(), null);
        }

        String resetToken = jwtUtil.generateVerificationToken(user.getId());
        userMapper.savePasswordResetToken(user.getId(), resetToken, LocalDateTime.now().plusHours(1));

        emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(),
                frontendUrl + "/reset-password?token=" + resetToken);

        return new DataResponse<>("Success", "Password reset link sent to your email", HttpStatus.OK.value(), null);
    }

    @Override
    public DataResponse<String> resetPassword(ResetPasswordRequest request) {
        if (!jwtUtil.validateToken(request.getToken())) {
            throw new BadRequestException();
        }

        Long userId = Long.valueOf(jwtUtil.extractUsername(request.getToken()));
        User user = userMapper.findUserById(userId);

        if (user == null) {
            throw new DataNotFoundException();
        }

        String hashedPassword = passwordEncoder.encode(request.getNewPassword());
        userMapper.updateUserPassword(userId, hashedPassword);
        userMapper.deletePasswordResetToken(userId);

        return new DataResponse<>("Success", "Password reset successfully", HttpStatus.OK.value(), null);
    }

    @Override
    public DataResponse<TokenResponse> refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtUtil.validateToken(refreshToken)) {
            throw new UnauthorizedException();
        }

        String tokenType = jwtUtil.extractTokenType(refreshToken);
        if (!REFRESH.equals(tokenType)) {
            throw new UnauthorizedException();
        }

        String username = jwtUtil.extractUsername(refreshToken);
        User user = userMapper.findUserByUsername(username);

        if (user == null || Boolean.TRUE.equals(!user.getIsActive())) {
            throw new UnauthorizedException();
        }

        List<Role> userRoles = userMapper.findUserRoles(user.getId());
        List<String> roleNames = userRoles.stream()
                .map(Role::getName)
                .collect(Collectors.toList());

        String newAccessToken = jwtUtil.generateToken(user.getUsername(), user.getFullName(), roleNames);
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        jwtUtil.blacklistToken(refreshToken);

        TokenResponse response = new TokenResponse();
        response.setToken(newAccessToken);
        response.setTokenType("Bearer");
        response.setRefreshToken(newRefreshToken);
        response.setExpiresIn(jwtExpiration);

        return new DataResponse<>("Success", "Token refreshed successfully", HttpStatus.OK.value(), response);
    }
}
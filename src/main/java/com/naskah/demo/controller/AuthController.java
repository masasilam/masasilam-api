package com.naskah.demo.controller;

import com.naskah.demo.model.dto.request.*;
import com.naskah.demo.model.dto.response.*;
import com.naskah.demo.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<DataResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        DataResponse<LoginResponse> response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<DataResponse<String>> logout(@RequestHeader("Authorization") String token) {
        DataResponse<String> response = authService.logout(token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<DataResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        DataResponse<RegisterResponse> response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/google")
    public ResponseEntity<DataResponse<LoginResponse>> googleAuth(@Valid @RequestBody GoogleAuthRequest request) {
        DataResponse<LoginResponse> response = authService.googleAuth(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<DataResponse<String>> verifyEmail(@RequestParam String token) {
        DataResponse<String> response = authService.verifyEmail(token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<DataResponse<String>> resendVerificationEmail(@RequestParam String email) {
        DataResponse<String> response = authService.resendVerificationEmail(email);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<DataResponse<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        DataResponse<String> response = authService.forgotPassword(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<DataResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        DataResponse<String> response = authService.resetPassword(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<DataResponse<TokenResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        DataResponse<TokenResponse> response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }
}
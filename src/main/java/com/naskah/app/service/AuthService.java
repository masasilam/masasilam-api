package com.naskah.app.service;

import com.naskah.app.model.dto.request.*;
import com.naskah.app.model.dto.response.DataResponse;
import com.naskah.app.model.dto.response.LoginResponse;
import com.naskah.app.model.dto.response.RegisterResponse;
import com.naskah.app.model.dto.response.TokenResponse;

public interface AuthService {
    DataResponse<LoginResponse> login(LoginRequest request);

    DataResponse<String> logout(String token);

    DataResponse<RegisterResponse> register(RegisterRequest request);

    DataResponse<LoginResponse> googleAuth(GoogleAuthRequest request);

    DataResponse<String> verifyEmail(String token);

    DataResponse<String> resendVerificationEmail(String email);

    DataResponse<String> forgotPassword(ForgotPasswordRequest request);

    DataResponse<String> resetPassword(ResetPasswordRequest request);

    DataResponse<TokenResponse> refreshToken(RefreshTokenRequest request);
}
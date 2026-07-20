package com.masasilam.app.service.common;

public interface EmailService {
    void sendVerificationEmail(String to, String name, String verificationLink);
    void sendPasswordResetEmail(String to, String name, String resetLink);
}
package com.naskah.demo.service;

public interface EmailService {
    void sendVerificationEmail(String to, String name, String verificationLink);

    void sendPasswordResetEmail(String to, String name, String resetLink);

    void sendWelcomeEmail(String to, String name);

    void sendContributionApprovedEmail(String to, String name, String bookTitle);
}
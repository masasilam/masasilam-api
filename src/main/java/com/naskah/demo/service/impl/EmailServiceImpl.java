package com.naskah.demo.service.impl;

import com.naskah.demo.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.name}")
    private String appName;

    @Override
    public void sendVerificationEmail(String to, String name, String verificationLink) {
        try {
            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("verificationLink", verificationLink);
            context.setVariable("appName", appName);

            String htmlContent = templateEngine.process("email/verification", context);

            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Verify Your " + appName + " Account");
            helper.setText(htmlContent, true);

            javaMailSender.send(message);
            log.info("Verification email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send verification email to: {}", to, e);
            throw new RuntimeException("Failed to send verification email");
        }
    }

    @Override
    public void sendPasswordResetEmail(String to, String name, String resetLink) {
        try {
            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("resetLink", resetLink);
            context.setVariable("appName", appName);

            String htmlContent = templateEngine.process("email/password-reset", context);

            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Reset Your " + appName + " Password");
            helper.setText(htmlContent, true);

            javaMailSender.send(message);
            log.info("Password reset email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send password reset email to: {}", to, e);
            throw new RuntimeException("Failed to send password reset email");
        }
    }

    @Override
    public void sendWelcomeEmail(String to, String name) {
        try {
            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("appName", appName);

            String htmlContent = templateEngine.process("email/welcome", context);

            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Welcome to " + appName + "!");
            helper.setText(htmlContent, true);

            javaMailSender.send(message);
            log.info("Welcome email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send welcome email to: {}", to, e);
        }
    }

    @Override
    public void sendContributionApprovedEmail(String to, String name, String bookTitle) {
        try {
            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("bookTitle", bookTitle);
            context.setVariable("appName", appName);

            String htmlContent = templateEngine.process("email/contribution-approved", context);

            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Your Contribution Has Been Approved!");
            helper.setText(htmlContent, true);

            javaMailSender.send(message);
            log.info("Contribution approved email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send contribution approved email to: {}", to, e);
        }
    }
}
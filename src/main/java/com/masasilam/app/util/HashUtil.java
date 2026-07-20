package com.masasilam.app.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtil {

    private HashUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static String generateSHA256(String input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();

        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }

        return hexString.toString();
    }

    public static String generateViewerHash(String slug, Long userId, String ipAddress, String userAgent) throws NoSuchAlgorithmException {
        String combined;
        if (userId != null) {
            combined = slug + ":user:" + userId;
        } else {
            combined = slug + ":guest:" + ipAddress + ":" + (userAgent != null ? userAgent : "");
        }
        return generateSHA256(combined);
    }
}
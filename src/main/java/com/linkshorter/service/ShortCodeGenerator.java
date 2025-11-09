package com.linkshorter.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;

/**
 * Generates unique short codes for URLs
 * Combines URL and user ID to ensure uniqueness per user
 */
public class ShortCodeGenerator {
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private final int codeLength;

    public ShortCodeGenerator(int codeLength) {
        if (codeLength <= 0) {
            throw new IllegalArgumentException("Code length must be positive");
        }
        this.codeLength = codeLength;
    }

    /**
     * Generate a short code for a URL and user combination
     * Ensures different users get different codes for the same URL
     */
    public String generateShortCode(String originalUrl, UUID userId) {
        if (originalUrl == null || originalUrl.isBlank()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        // Combine URL and user ID to ensure uniqueness per user
        String combined = originalUrl + "|" + userId.toString();

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(combined.getBytes(StandardCharsets.UTF_8));

            // Convert hash to base62-like encoding
            return encodeToAlphabet(hash, codeLength);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private String encodeToAlphabet(byte[] data, int length) {
        StringBuilder result = new StringBuilder();

        // Use the hash bytes to select characters from alphabet
        for (int i = 0; i < length && i < data.length; i++) {
            int index = Math.abs(data[i]) % ALPHABET.length();
            result.append(ALPHABET.charAt(index));
        }

        // If we need more characters, use additional hash bytes
        if (result.length() < length) {
            for (int i = data.length; result.length() < length; i++) {
                int index = Math.abs(data[i % data.length] + i) % ALPHABET.length();
                result.append(ALPHABET.charAt(index));
            }
        }

        return result.toString();
    }

    /**
     * Validate if a short code has the correct format
     */
    public boolean isValidShortCode(String code) {
        if (code == null || code.length() != codeLength) {
            return false;
        }

        for (char c : code.toCharArray()) {
            if (ALPHABET.indexOf(c) == -1) {
                return false;
            }
        }

        return true;
    }
}


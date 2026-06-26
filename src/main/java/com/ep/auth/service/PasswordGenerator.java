package com.ep.auth.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class PasswordGenerator {
    private static final char[] CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%^&*".toCharArray();
    private final SecureRandom secureRandom = new SecureRandom();

    public String temporaryPassword() {
        StringBuilder value = new StringBuilder(18);
        for (int i = 0; i < 18; i++) {
            value.append(CHARS[secureRandom.nextInt(CHARS.length)]);
        }
        return value.toString();
    }
}

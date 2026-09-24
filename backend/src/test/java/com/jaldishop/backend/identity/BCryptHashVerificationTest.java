package com.jaldishop.backend.identity;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BCryptHashVerificationTest {

    @Test
    void testPasswordMatches() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String raw = "Password123!";
        String encoded = encoder.encode(raw);
        System.out.println("GENERATED_HASH=" + encoded);
        assertTrue(encoder.matches(raw, encoded));
        
        String newHash = "$2a$10$/hcyKr9yCPUSEREMAygtg.GymkhPi5aOdaepBK8AV/Ojh5Y0uohfS";
        assertTrue(encoder.matches(raw, newHash));
    }
}

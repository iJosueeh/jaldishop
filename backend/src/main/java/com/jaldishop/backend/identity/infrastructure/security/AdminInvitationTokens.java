package com.jaldishop.backend.identity.infrastructure.security;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class AdminInvitationTokens {

    private final OpaqueTokenCodec codec = new OpaqueTokenCodec();

    public String generate() {
        return codec.generate();
    }

    public String hash(String token) {
        return codec.hash(token);
    }
}

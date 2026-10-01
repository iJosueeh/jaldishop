package com.jaldishop.backend.media.domain;

public record UploadSignature(
        String cloudName,
        String apiKey,
        long timestamp,
        String folder,
        String signature,
        String uploadUrl
) {}

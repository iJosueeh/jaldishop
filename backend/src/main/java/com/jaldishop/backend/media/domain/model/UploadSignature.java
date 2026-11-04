package com.jaldishop.backend.media.domain.model;

public record UploadSignature(
        String cloudName,
        String apiKey,
        long timestamp,
        String folder,
        String signature,
        String uploadUrl
) {}

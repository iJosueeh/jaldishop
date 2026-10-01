package com.jaldishop.backend.media.web.dto;

import com.jaldishop.backend.media.domain.UploadSignature;

public record UploadSignatureResponse(
        String cloudName,
        String apiKey,
        long timestamp,
        String folder,
        String signature,
        String uploadUrl
) {
    public static UploadSignatureResponse fromDomain(UploadSignature domain) {
        return new UploadSignatureResponse(
                domain.cloudName(),
                domain.apiKey(),
                domain.timestamp(),
                domain.folder(),
                domain.signature(),
                domain.uploadUrl()
        );
    }
}

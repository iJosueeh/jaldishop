package com.jaldishop.backend.media.application.service;

import com.jaldishop.backend.catalog.domain.port.CatalogMediaCleaner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CatalogMediaCleanupAdapter implements CatalogMediaCleaner {

    private static final Pattern CLOUDINARY_PUBLIC_ID_PATTERN =
            Pattern.compile(".*/upload/(?:v\\d+/)?(.+?)(?:\\.[a-zA-Z0-9]+)?$");

    private final MediaCleanupOutboxService outboxService;

    public CatalogMediaCleanupAdapter(MediaCleanupOutboxService outboxService) {
        this.outboxService = outboxService;
    }

    @Override
    public void enqueueAssetDeletions(List<String> imageUrlsOrPublicIds) {
        if (imageUrlsOrPublicIds == null || imageUrlsOrPublicIds.isEmpty()) {
            return;
        }
        List<String> publicIds = imageUrlsOrPublicIds.stream()
                .filter(v -> v != null && !v.isBlank())
                .map(this::extractPublicId)
                .filter(v -> v != null && !v.isBlank())
                .toList();
        outboxService.enqueuePublicIds(publicIds);
    }

    private String extractPublicId(String value) {
        if (!value.startsWith("http")) {
            return value;
        }
        Matcher matcher = CLOUDINARY_PUBLIC_ID_PATTERN.matcher(value);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }
}

package com.jaldishop.backend.catalog.domain.port;

import java.util.List;

public interface CatalogMediaCleaner {
    void enqueueAssetDeletions(List<String> imageUrlsOrPublicIds);
}

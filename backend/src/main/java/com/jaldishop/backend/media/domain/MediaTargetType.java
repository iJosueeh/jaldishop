package com.jaldishop.backend.media.domain;

public enum MediaTargetType {
    STORE_LOGO("branding"),
    STORE_BANNER("branding"),
    PRODUCT_IMAGE("products");

    private final String subfolder;

    MediaTargetType(String subfolder) {
        this.subfolder = subfolder;
    }

    public String getSubfolder() {
        return subfolder;
    }
}

package de.rtcustomz.walloflight.model;

import androidx.annotation.NonNull;

@SuppressWarnings("unused")
public enum ImageType {
    ANY("image/*"),
    GIF("image/gif"),
    JPEG("image/jpeg"),
    PNG("image/png");

    private final String mimeType;

    ImageType(String mimeType) {
        this.mimeType = mimeType;
    }

    @Override
    @NonNull
    public String toString() {
        return this.mimeType;
    }
}

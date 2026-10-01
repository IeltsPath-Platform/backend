package com.group01.content.domain.vo;

import com.group01.content.domain.exception.InvalidMediaReferenceException;

import java.util.regex.Pattern;

/**
 * Turns a stored {@code media_reference} into the URL a client may load, and rejects anything a browser could run or
 * that points inside the network ({@code javascript:}, {@code file:}, relative paths). Only images are supported so
 * far; other asset types are rejected until they get their own rule.
 */
public final class MediaReferencePolicy {

    private static final Pattern IMAGE_REFERENCE = Pattern.compile(
            "^(https://\\S+|data:image/(png|jpeg|svg\\+xml);base64,[A-Za-z0-9+/]+={0,2})$");

    private MediaReferencePolicy() {
    }

    public static String resolve(AssetType assetType, String mediaReference) {
        if (assetType == AssetType.IMAGE && mediaReference != null && IMAGE_REFERENCE.matcher(mediaReference).matches()) {
            return mediaReference;
        }
        throw new InvalidMediaReferenceException(assetType);
    }
}

package com.ieltspath.content.domain.vo;

import com.ieltspath.content.domain.exception.InvalidMediaReferenceException;

import java.util.regex.Pattern;

/**
 * Turns a stored {@code media_reference} into the URL a client may load, and rejects anything a browser could run or
 * that points inside the network ({@code javascript:}, {@code file:}, absolute or {@code ..} paths). Content is the only
 * place that builds media URLs, so moving files to another bucket only changes the base URL.
 */
public final class MediaReferencePolicy {

    private static final Pattern HTTPS_URL = Pattern.compile("^https://\\S+$");
    private static final Pattern IMAGE_DATA_URI =
            Pattern.compile("^data:image/(png|jpeg|svg\\+xml);base64,[A-Za-z0-9+/]+={0,2}$");
    /** A relative object key such as {@code listening/demo/ls1.mp3}: no scheme, no leading slash. */
    private static final Pattern OBJECT_KEY = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]*(/[A-Za-z0-9._-]+)*$");

    private final String baseUrl;

    /** @param baseUrl prefix for object keys, an https URL or blank when media is only referenced by full URL */
    public MediaReferencePolicy(String baseUrl) {
        String base = baseUrl == null ? "" : baseUrl.strip();
        if (!base.isEmpty() && !HTTPS_URL.matcher(base).matches()) {
            throw new IllegalArgumentException("Media base URL must be an https URL");
        }
        this.baseUrl = base.isEmpty() || base.endsWith("/") ? base : base + "/";
    }

    public String resolve(AssetType assetType, String mediaReference) {
        if (mediaReference != null && assetType != null) {
            if (HTTPS_URL.matcher(mediaReference).matches()) {
                return mediaReference;
            }
            if (assetType == AssetType.IMAGE && IMAGE_DATA_URI.matcher(mediaReference).matches()) {
                return mediaReference;
            }
            if (assetType == AssetType.AUDIO && !baseUrl.isEmpty() && isObjectKey(mediaReference)) {
                return baseUrl + mediaReference;
            }
        }
        throw new InvalidMediaReferenceException(assetType);
    }

    private static boolean isObjectKey(String reference) {
        return OBJECT_KEY.matcher(reference).matches() && !reference.contains("..");
    }
}

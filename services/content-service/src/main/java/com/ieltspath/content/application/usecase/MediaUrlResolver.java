package com.ieltspath.content.application.usecase;

import com.ieltspath.commonstorage.ObjectStorage;
import com.ieltspath.content.domain.vo.AccessTier;
import com.ieltspath.content.domain.vo.AssetType;
import com.ieltspath.content.domain.vo.MediaReferencePolicy;

import java.time.Duration;

/** Resolves stored media references for internal consumers, signing private audio only for lesson playback. */
public final class MediaUrlResolver {
    private final MediaReferencePolicy publicMedia;
    private final ObjectStorage storage;
    private final String premiumBucket;
    private final Duration signedUrlTtl;

    public MediaUrlResolver(MediaReferencePolicy publicMedia, ObjectStorage storage, String premiumBucket,
                            Duration signedUrlTtl) {
        this.publicMedia = publicMedia;
        this.storage = storage;
        this.premiumBucket = premiumBucket;
        this.signedUrlTtl = signedUrlTtl;
    }

    public String resolve(AssetType type, String reference, AccessTier tier, boolean signedAudio) {
        if (signedAudio && type == AssetType.AUDIO && tier == AccessTier.PREMIUM
                && reference != null && reference.startsWith("https://")) {
            return publicMedia.resolve(type, reference);
        }
        if (signedAudio && type == AssetType.AUDIO && tier == AccessTier.PREMIUM) {
            // Reuse the public policy to validate the key before asking storage to sign it.
            publicMedia.resolve(type, reference);
            return storage.signedDownloadUrl(premiumBucket, reference, signedUrlTtl);
        }
        return publicMedia.resolve(type, reference);
    }
}

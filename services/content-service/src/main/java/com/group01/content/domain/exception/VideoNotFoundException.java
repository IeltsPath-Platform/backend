package com.group01.content.domain.exception;

import java.util.UUID;

public class VideoNotFoundException extends ContentDomainException {
    public VideoNotFoundException(UUID id) {
        super("Learning video not found with id: " + id);
    }

    public VideoNotFoundException(String youtubeVideoId) {
        super("Learning video not found with youtube id: " + youtubeVideoId);
    }
}


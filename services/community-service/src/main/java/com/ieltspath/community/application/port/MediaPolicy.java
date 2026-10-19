package com.ieltspath.community.application.port;

import java.util.List;
import java.util.UUID;

public interface MediaPolicy {

    /** Folder (inside the bucket) of images uploaded through the service: {@code community/post/{userId}/{uuid}.{ext}}. */
    String IMAGE_KEY_PREFIX = "community/post";

    void validate(UUID authorId, List<String> images, String video);
}

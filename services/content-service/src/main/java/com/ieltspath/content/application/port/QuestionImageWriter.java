package com.ieltspath.content.application.port;

import java.util.UUID;

/**
 * Sets or removes the prompt image of a question version (the chart of a Writing task). A question version shows one
 * image, so a replacement unlinks the old one and links a new asset. Old assets are kept: another version may use them.
 */
public interface QuestionImageWriter {

    void replace(UUID questionVersionId, String mediaReference, String altText);

    void clear(UUID questionVersionId);

    /**
     * Gives a new version the prompt image of the version before it: the highest-numbered other version of the same
     * question that has one. The asset is shared, not duplicated; nothing happens when no earlier version has an image.
     */
    void copyFromPreviousVersion(UUID newQuestionVersionId);
}

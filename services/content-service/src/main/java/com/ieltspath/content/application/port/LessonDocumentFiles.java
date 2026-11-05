package com.ieltspath.content.application.port;

/**
 * The files of uploaded lesson documents. They live in a private bucket, so a learner never gets a permanent link:
 * the API signs a short-lived one each time it lists a lesson's resources.
 */
public interface LessonDocumentFiles {
    /** Whether the key names a stored file. Call it before trusting a key the client sends back. */
    boolean exists(String fileKey);

    /** Removes the file. A missing file is not an error. */
    void delete(String fileKey);

    /** A download link that expires on its own. It is a secret: never log it. */
    String signedUrl(String fileKey);
}

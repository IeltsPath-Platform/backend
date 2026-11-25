package com.ieltspath.content.application.port;

import java.util.UUID;

/**
 * Copies the content of one package version into another, for a draft that starts from the version before it. Sections,
 * question groups, question links and the passage and audio links are copied with new ids; the questions and assets
 * themselves are shared, not duplicated.
 */
public interface PackageVersionCopier {

    void copyContent(UUID sourceVersionId, UUID targetVersionId);
}

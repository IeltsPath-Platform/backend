package com.ieltspath.content.application.port;

import java.util.UUID;

/**
 * Replaces the passage or the audio of a section. A section shows the first linked asset of each kind, so a
 * replacement removes the old links and links a new asset. Old assets are kept: a published version may still use them.
 */
public interface SectionMediaWriter {

    void setPassage(UUID sectionId, String text);

    void clearPassage(UUID sectionId);

    /** @return the id of the new audio asset */
    UUID setAudio(UUID sectionId, String mediaReference, int durationSeconds, String transcript);

    void clearAudio(UUID sectionId);
}

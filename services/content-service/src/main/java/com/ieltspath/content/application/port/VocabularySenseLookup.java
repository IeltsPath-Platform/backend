package com.ieltspath.content.application.port;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface VocabularySenseLookup {
    Set<UUID> findActiveSenseIds(List<UUID> senseIds);

    /** The word and Vietnamese meaning of an active sense, for text that quotes the lesson's vocabulary. */
    record SenseText(UUID senseId, String lemma, String vietnameseMeaning) {
    }

    /**
     * The active senses among {@code senseIds} with their words. A lookup that cannot say more than which senses are
     * active returns nothing, and the text that would quote them simply leaves them out.
     */
    default List<SenseText> describeActiveSenses(List<UUID> senseIds) {
        return List.of();
    }
}

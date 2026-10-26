package com.ieltspath.content.application.port;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface VocabularySenseLookup {
    Set<UUID> findActiveSenseIds(List<UUID> senseIds);
}

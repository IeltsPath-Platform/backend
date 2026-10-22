package com.ieltspath.library.infrastructure.persistence.repository;

import java.util.UUID;

public interface DeckCardCountProjection {
    UUID getDeckId();
    long getCardCount();
    long getDueCount();
}

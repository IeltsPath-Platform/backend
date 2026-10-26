package com.ieltspath.game.infrastructure.persistence.entity;

import static org.junit.jupiter.api.Assertions.*;

import com.ieltspath.game.domain.vo.RocketRoomSettings;
import com.ieltspath.game.domain.vo.WordlyRoomSettings;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GameRoomJpaEntitySettingsTest {
    private GameRoomJpaEntity room(String gameType) {
        return new GameRoomJpaEntity(UUID.randomUUID(), "ABC123", UUID.randomUUID(), gameType, "VOCABULARY", "PRACTICE",
                4, Map.of(), Instant.parse("2026-10-22T00:00:00Z"), null);
    }

    @Test
    void rocketSettingsRoundTrip() {
        var entity = room("ROCKET");
        var solo = RocketRoomSettings.solo(8);
        entity.setSettings(solo);
        assertEquals(solo, entity.settings());
        var multi = RocketRoomSettings.multiplayer(12, 5, 20);
        entity.setSettings(multi);
        assertEquals(multi, entity.settings());
        assertNull(entity.wordlySettings());
    }

    @Test
    void wordlySettingsStillRoundTripAndLegacyRoomsHaveNone() {
        var entity = room("WORDLY");
        var settings = new WordlyRoomSettings(WordlyRoomSettings.PlayerMode.MULTIPLAYER, 5,
                WordlyRoomSettings.PlayMode.ENDLESS, null);
        entity.setSettings(settings);
        assertEquals(settings, entity.settings());
        assertEquals(settings, entity.wordlySettings());
        entity.setSettings(null);
        assertEquals(settings, entity.settings());
        assertNull(room("WORD_MEANING_MATCH").settings());
    }
}

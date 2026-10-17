package com.ieltspath.game.application.port;
import com.ieltspath.game.application.result.CurrentWordResult;
import java.util.List;
import java.util.UUID;
import java.time.Instant;
import java.util.Optional;
public interface WordlyMatchStore {
    void initialize(UUID matchId,List<WordlyWordProvider.Word> words,List<UUID> players,Instant now,boolean timed);
    void preparePool(UUID matchId,List<WordlyWordProvider.Word> words);
    Optional<CurrentWordResult> current(UUID matchId,UUID userId);
}

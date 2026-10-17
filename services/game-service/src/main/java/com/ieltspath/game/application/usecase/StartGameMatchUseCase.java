package com.ieltspath.game.application.usecase;

import com.ieltspath.game.application.port.OutboxWriter;
import com.ieltspath.game.application.result.GameMatchResult;
import com.ieltspath.game.domain.aggregate.GameMatch;
import com.ieltspath.game.domain.aggregate.GameMatchPlayer;
import com.ieltspath.game.domain.aggregate.GameRoom;
import com.ieltspath.game.domain.aggregate.GameSession;
import com.ieltspath.game.domain.exception.GameRoomNotFoundException;
import com.ieltspath.game.domain.repository.GameMatchPlayerRepository;
import com.ieltspath.game.domain.repository.GameMatchRepository;
import com.ieltspath.game.domain.repository.GameRoomRepository;
import com.ieltspath.game.domain.repository.GameSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class StartGameMatchUseCase {
    private final org.springframework.context.ApplicationEventPublisher events;
    private final GameRoomRepository roomRepository;
    private final GameMatchRepository matchRepository;
    private final GameMatchPlayerRepository playerRepository;
    private final GameSessionRepository sessionRepository;
    private final com.ieltspath.game.application.port.WordlyWordProvider wordProvider;
    private final com.ieltspath.game.application.port.WordlyMatchStore wordStore;
    private final OutboxWriter outboxWriter;
    public StartGameMatchUseCase(GameRoomRepository roomRepository, GameMatchRepository matchRepository,
            GameMatchPlayerRepository playerRepository, GameSessionRepository sessionRepository, OutboxWriter outboxWriter, org.springframework.context.ApplicationEventPublisher events, com.ieltspath.game.application.port.WordlyWordProvider wordProvider, com.ieltspath.game.application.port.WordlyMatchStore wordStore) {
        this.wordProvider=wordProvider;this.wordStore=wordStore;
        this.events = events;
        this.roomRepository=roomRepository; this.matchRepository=matchRepository; this.playerRepository=playerRepository;
        this.sessionRepository=sessionRepository; this.outboxWriter=outboxWriter;
    }

    @Transactional
    public GameMatchResult execute(UUID roomId, UUID hostUserId) {
        GameRoom room=roomRepository.findForUpdate(roomId).orElseThrow(()->new GameRoomNotFoundException(roomId));
        Instant now=Instant.now();
        room.startMatch(hostUserId,now);
        List<com.ieltspath.game.application.port.WordlyWordProvider.Word> words=List.of();
        List<com.ieltspath.game.application.port.WordlyWordProvider.Word> pool=List.of();
        if(room.wordlySettings()!=null) {
            int count=room.wordlySettings().wordLimit()==null?10:room.wordlySettings().wordLimit();
            words=wordProvider.random(count);
            if(words.isEmpty() || (room.wordlySettings().wordLimit()!=null && words.size()!=count))
                throw new IllegalStateException("Not enough eligible vocabulary to start this match");
            if(words.stream().map(w->w.answer()).distinct().count()!=words.size()
                || words.stream().anyMatch(w->w.sourceWordId()==null || w.answer()==null || !w.answer().matches("[A-Z]{1,100}") || w.meaning()==null || w.meaning().isBlank()))
                throw new IllegalStateException("Invalid Wordly vocabulary snapshot");
            {
                var catalog=new java.util.ArrayList<>(wordProvider.catalog());
                java.util.Collections.shuffle(catalog);
                var unique=new java.util.LinkedHashMap<String,com.ieltspath.game.application.port.WordlyWordProvider.Word>();
                for(var w:words)unique.put(w.answer(),w);
                for(var w:catalog) {
                    if(w.answer()==null||!w.answer().matches("[A-Z]{1,100}")||w.meaning()==null||w.meaning().isBlank()||w.sourceWordId()==null)
                        throw new IllegalStateException("Invalid vocabulary catalog");
                    unique.putIfAbsent(w.answer(),w);
                }
                pool=new java.util.ArrayList<>(unique.values());
            }
            now=Instant.now();
        }
        roomRepository.save(room);
        UUID matchId=UUID.randomUUID();
        Map<String,Object> matchConfig = new java.util.LinkedHashMap<>();
        matchConfig.put("mode", room.mode());
        if (room.wordlySettings() == null) {
            matchConfig.put("scoring", room.configSnapshot().getOrDefault("scoring", Map.of()));
        } else {
            var settings = room.wordlySettings();
            matchConfig.put("playerMode", settings.playerMode().name());
            matchConfig.put("playMode", settings.playMode().name());
            matchConfig.put("initialLives", settings.initialLives());
            matchConfig.put("timeEnabled", settings.timeEnabled());
            if (settings.timeEnabled()) matchConfig.put("wordTimeLimitSeconds", 180);
            if (settings.wordLimit() != null) matchConfig.put("wordLimit", settings.wordLimit());
        }
        GameMatch match=matchRepository.save(new GameMatch(matchId,roomId,room.gameType(),room.learningDomain(),
                matchConfig,room.configSnapshot(),now));
        List<UUID> playerIds=new java.util.ArrayList<>();
        for(GameRoom.Member member:room.members().values()) {
            if(!member.active()) continue;
            UUID playerId=UUID.randomUUID();
            GameMatchPlayer player=playerRepository.save(new GameMatchPlayer(playerId,matchId,member.id(),member.userId(),
                    0,null,GameMatchPlayer.Status.ACTIVE,now,null,room.wordlySettings() == null ? 4 : room.wordlySettings().initialLives()));
            playerIds.add(playerId);
            if (room.wordlySettings() != null) continue;
            Map<String,Object> snapshot=room.configSnapshot();
            GameSession session=new GameSession(UUID.randomUUID(),member.userId(),null,playerId,room.gameType(),
                    room.learningDomain(),room.mode(),now,snapshot,"PENDING");
            sessionRepository.save(session);
        }
        if(room.wordlySettings()!=null) wordStore.initialize(matchId,words,playerIds,now,room.wordlySettings().timeEnabled());
        if(!pool.isEmpty())wordStore.preparePool(matchId,pool);
        outboxWriter.append("GameMatch",matchId.toString(),"GameMatchStarted",
                Map.of("matchId",matchId.toString(),"roomId",roomId.toString()));
        events.publishEvent(new com.ieltspath.game.application.result.MatchStartedEvent(roomId, matchId));
        return result(match,hostUserId);
    }

    private GameMatchResult result(GameMatch match,UUID userId) {
        List<GameMatchPlayer> players=playerRepository.findByMatchId(match.id());
        UUID playerId=players.stream().filter(p->p.userId().equals(userId)).map(GameMatchPlayer::id).findFirst().orElseThrow();
        UUID sessionId="WORDLY".equals(match.gameType()) ? null : sessionRepository.findByMatchPlayerId(playerId).orElseThrow().id();
        return new GameMatchResult(match.id(),match.roomId(),sessionId,match.status().name(),match.startedAt(),GameMatchResult.players(players));
    }
}

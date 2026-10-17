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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Clock;
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
    private final TransactionTemplate tx;
    private final Clock clock;
    public StartGameMatchUseCase(GameRoomRepository roomRepository, GameMatchRepository matchRepository,
            GameMatchPlayerRepository playerRepository, GameSessionRepository sessionRepository, OutboxWriter outboxWriter, org.springframework.context.ApplicationEventPublisher events, com.ieltspath.game.application.port.WordlyWordProvider wordProvider, com.ieltspath.game.application.port.WordlyMatchStore wordStore, PlatformTransactionManager manager, Clock clock) {
        this.tx=new TransactionTemplate(manager); this.clock=clock;
        this.wordProvider=wordProvider;this.wordStore=wordStore;
        this.events = events;
        this.roomRepository=roomRepository; this.matchRepository=matchRepository; this.playerRepository=playerRepository;
        this.sessionRepository=sessionRepository; this.outboxWriter=outboxWriter;
    }

    public GameMatchResult execute(UUID roomId, UUID hostUserId) {
        GameRoom preview=roomRepository.findById(roomId).orElseThrow(()->new GameRoomNotFoundException(roomId));
        // Validate before remote work; repeat under the lock because readiness/settings may change.
        preview.startMatch(hostUserId,clock.instant());
        var settings=preview.wordlySettings();
        List<com.ieltspath.game.application.port.WordlyWordProvider.Word> words=List.of();
        String dictionary=null;
        if(settings!=null) {
            var catalog=wordProvider.catalog();
            var unique=new java.util.TreeMap<String,com.ieltspath.game.application.port.WordlyWordProvider.Word>();
            for(var w:catalog) {
                if(w.sourceWordId()==null || w.answer()==null || !w.answer().matches("[A-Z]{1,100}") || w.meaning()==null || w.meaning().isBlank())
                    throw new IllegalStateException("Invalid vocabulary catalog");
                unique.putIfAbsent(w.answer(),w);
            }
            var candidates=new java.util.ArrayList<>(unique.values());
            int count=settings.wordLimit()==null?Math.min(10,candidates.size()):settings.wordLimit();
            if(count<1 || candidates.size()<count)
                throw new IllegalStateException("Not enough eligible vocabulary to start this match");
            java.util.Collections.shuffle(candidates);
            words=List.copyOf(candidates.subList(0,count));
            dictionary=wordStore.prepareDictionary(List.copyOf(unique.values()));
        }
        var preparedWords=words;
        var preparedDictionary=dictionary;
        return tx.execute(status -> {
            GameRoom room=roomRepository.findForUpdate(roomId).orElseThrow(()->new GameRoomNotFoundException(roomId));
            if(!java.util.Objects.equals(settings,room.wordlySettings()))
                throw new com.ieltspath.game.domain.exception.InvalidGameRoomStateException("Room settings changed; retry start");
            Instant now=clock.instant();
            room.startMatch(hostUserId,now);
            return start(room,hostUserId,preparedWords,preparedDictionary,now);
        });
    }

    private GameMatchResult start(GameRoom room, UUID hostUserId,
            List<com.ieltspath.game.application.port.WordlyWordProvider.Word> words,String dictionary,Instant now) {
        UUID roomId=room.id();
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
        if(dictionary!=null)wordStore.attachDictionary(matchId,dictionary);
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

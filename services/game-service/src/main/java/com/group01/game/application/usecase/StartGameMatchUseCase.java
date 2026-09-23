package com.group01.game.application.usecase;

import com.group01.game.application.port.OutboxWriter;
import com.group01.game.application.result.GameMatchResult;
import com.group01.game.domain.aggregate.GameMatch;
import com.group01.game.domain.aggregate.GameMatchPlayer;
import com.group01.game.domain.aggregate.GameRoom;
import com.group01.game.domain.aggregate.GameSession;
import com.group01.game.domain.exception.GameRoomNotFoundException;
import com.group01.game.domain.repository.GameMatchPlayerRepository;
import com.group01.game.domain.repository.GameMatchRepository;
import com.group01.game.domain.repository.GameRoomRepository;
import com.group01.game.domain.repository.GameSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class StartGameMatchUseCase {
    private final GameRoomRepository roomRepository;
    private final GameMatchRepository matchRepository;
    private final GameMatchPlayerRepository playerRepository;
    private final GameSessionRepository sessionRepository;
    private final OutboxWriter outboxWriter;
    public StartGameMatchUseCase(GameRoomRepository roomRepository, GameMatchRepository matchRepository,
            GameMatchPlayerRepository playerRepository, GameSessionRepository sessionRepository, OutboxWriter outboxWriter) {
        this.roomRepository=roomRepository; this.matchRepository=matchRepository; this.playerRepository=playerRepository;
        this.sessionRepository=sessionRepository; this.outboxWriter=outboxWriter;
    }

    @Transactional
    public GameMatchResult execute(UUID roomId, UUID hostUserId) {
        GameRoom room=roomRepository.findForUpdate(roomId).orElseThrow(()->new GameRoomNotFoundException(roomId));
        Instant now=Instant.now();
        room.startMatch(hostUserId,now);
        roomRepository.save(room);
        UUID matchId=UUID.randomUUID();
        Map<String,Object> matchConfig=Map.of("mode",room.mode(),"scoring",room.configSnapshot().get("scoring"));
        GameMatch match=matchRepository.save(new GameMatch(matchId,roomId,room.gameType(),room.learningDomain(),
                matchConfig,room.configSnapshot(),now));
        for(GameRoom.Member member:room.members().values()) {
            if(!member.active()) continue;
            UUID playerId=UUID.randomUUID();
            GameMatchPlayer player=playerRepository.save(new GameMatchPlayer(playerId,matchId,member.id(),member.userId(),
                    0,null,GameMatchPlayer.Status.ACTIVE,now,null));
            Map<String,Object> snapshot=room.configSnapshot();
            GameSession session=new GameSession(UUID.randomUUID(),member.userId(),null,playerId,room.gameType(),
                    room.learningDomain(),room.mode(),now,snapshot,"PENDING");
            sessionRepository.save(session);
        }
        outboxWriter.append("GameMatch",matchId.toString(),"GameMatchStarted",
                Map.of("matchId",matchId.toString(),"roomId",roomId.toString()));
        return result(match,hostUserId);
    }

    private GameMatchResult result(GameMatch match,UUID userId) {
        List<GameMatchPlayer> players=playerRepository.findByMatchId(match.id());
        UUID playerId=players.stream().filter(p->p.userId().equals(userId)).map(GameMatchPlayer::id).findFirst().orElseThrow();
        UUID sessionId=sessionRepository.findByMatchPlayerId(playerId).orElseThrow().id();
        return new GameMatchResult(match.id(),match.roomId(),sessionId,match.status().name(),match.startedAt(),GameMatchResult.players(players));
    }
}

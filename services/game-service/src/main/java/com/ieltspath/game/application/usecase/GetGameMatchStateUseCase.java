package com.ieltspath.game.application.usecase;

import com.ieltspath.game.application.result.GameMatchResult;
import com.ieltspath.game.domain.exception.GameMatchNotFoundException;
import com.ieltspath.game.domain.repository.GameMatchPlayerRepository;
import com.ieltspath.game.domain.repository.GameMatchRepository;
import com.ieltspath.game.domain.repository.GameSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class GetGameMatchStateUseCase {
    private final GameMatchRepository matchRepository;
    private final GameMatchPlayerRepository playerRepository;
    private final GameSessionRepository sessionRepository;
    private final com.ieltspath.game.application.port.WordlyRankingRepository rankings;
    public GetGameMatchStateUseCase(GameMatchRepository matchRepository,GameMatchPlayerRepository playerRepository,GameSessionRepository sessionRepository, com.ieltspath.game.application.port.WordlyRankingRepository rankings){this.matchRepository=matchRepository;this.playerRepository=playerRepository;this.sessionRepository=sessionRepository;this.rankings=rankings;}
    @Transactional(readOnly=true)
    public GameMatchResult execute(UUID matchId,UUID userId){
        var match=matchRepository.findById(matchId).orElseThrow(()->new GameMatchNotFoundException(matchId));
        var player=playerRepository.findByMatchIdAndUserId(matchId,userId).orElseThrow(()->new GameMatchNotFoundException(matchId));
        UUID sessionId = com.ieltspath.game.domain.vo.RoomGame.isRoomGame(match.gameType()) ? null : sessionRepository.findByMatchPlayerId(player.id()).orElseThrow(()->new GameMatchNotFoundException(matchId)).id();
        var ranking=com.ieltspath.game.domain.vo.RoomGame.isRoomGame(match.gameType()) ? rankings.find(matchId,userId).orElseThrow(()->new GameMatchNotFoundException(matchId)) : null;
        return new GameMatchResult(match.id(),match.roomId(),sessionId,match.status().name(),match.startedAt(),GameMatchResult.players(playerRepository.findByMatchId(matchId)),
            ranking==null?null:ranking.playerMode(),ranking==null?null:match.configSnapshot(),
            ranking==null?null:ranking.players().stream().filter(p->p.userId().equals(userId)).findFirst().orElseThrow(),ranking);
    }
}

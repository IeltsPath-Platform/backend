package com.group01.game.application.usecase;

import com.group01.game.application.result.GameMatchResult;
import com.group01.game.domain.exception.GameMatchNotFoundException;
import com.group01.game.domain.repository.GameMatchPlayerRepository;
import com.group01.game.domain.repository.GameMatchRepository;
import com.group01.game.domain.repository.GameSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class GetGameMatchStateUseCase {
    private final GameMatchRepository matchRepository;
    private final GameMatchPlayerRepository playerRepository;
    private final GameSessionRepository sessionRepository;
    public GetGameMatchStateUseCase(GameMatchRepository matchRepository,GameMatchPlayerRepository playerRepository,GameSessionRepository sessionRepository){this.matchRepository=matchRepository;this.playerRepository=playerRepository;this.sessionRepository=sessionRepository;}
    @Transactional(readOnly=true)
    public GameMatchResult execute(UUID matchId,UUID userId){
        var match=matchRepository.findById(matchId).orElseThrow(()->new GameMatchNotFoundException(matchId));
        var player=playerRepository.findByMatchIdAndUserId(matchId,userId).orElseThrow(()->new GameMatchNotFoundException(matchId));
        var session=sessionRepository.findByMatchPlayerId(player.id()).orElseThrow(()->new GameMatchNotFoundException(matchId));
        return new GameMatchResult(match.id(),match.roomId(),session.id(),match.status().name(),match.startedAt(),GameMatchResult.players(playerRepository.findByMatchId(matchId)));
    }
}

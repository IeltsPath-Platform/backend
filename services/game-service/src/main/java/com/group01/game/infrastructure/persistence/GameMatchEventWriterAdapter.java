package com.group01.game.infrastructure.persistence;

import com.group01.game.application.port.GameMatchEventWriter;
import com.group01.game.domain.aggregate.GameAnswer;
import com.group01.game.domain.aggregate.GameMatch;
import com.group01.game.domain.aggregate.GameMatchPlayer;
import com.group01.game.domain.aggregate.GameSession;
import com.group01.game.domain.repository.GameMatchPlayerRepository;
import com.group01.game.domain.repository.GameMatchRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;

@Repository
public class GameMatchEventWriterAdapter implements GameMatchEventWriter {
    private final GameMatchPlayerJpaRepository playerJpaRepository;
    private final GameMatchJpaRepository matchJpaRepository;
    private final GameEventJpaRepository eventJpaRepository;
    private final GameMatchRepository matchRepository;

    public GameMatchEventWriterAdapter(GameMatchPlayerJpaRepository playerJpaRepository,
            GameMatchJpaRepository matchJpaRepository, GameEventJpaRepository eventJpaRepository,
            GameMatchRepository matchRepository) {
        this.playerJpaRepository=playerJpaRepository; this.matchJpaRepository=matchJpaRepository;
        this.eventJpaRepository=eventJpaRepository; this.matchRepository=matchRepository;
    }

    @Override
    @Transactional
    public boolean recordAnswer(GameAnswer answer, GameSession session) {
        UUID playerId=session.matchPlayerId();
        GameMatchPlayerJpaEntity player=playerJpaRepository.findById(playerId).orElseThrow();
        UUID matchId=player.getMatchId();
        GameMatchJpaEntity match=matchJpaRepository.findByIdForUpdate(matchId).orElseThrow();
        if (!"IN_PROGRESS".equals(match.getStatus())) throw new IllegalStateException("Match is no longer in progress");
        Instant now=Instant.now();
        long sequence=eventJpaRepository.findMaxSequence(matchId)+1;
        eventJpaRepository.save(new GameEventJpaEntity(UUID.randomUUID(),matchId,playerId,sequence,
                "ANSWER_SUBMITTED",Map.of("sessionId",session.id().toString(),"itemSequence",answer.itemSequence(),
                        "isCorrect",answer.correct(),"score",session.score()),now));
        player.updateProgress(session.score(),"COMPLETED".equals(session.status().name()),now);
        playerJpaRepository.save(player);
        boolean completed=playerJpaRepository.countByMatchIdAndStatusNot(matchId,"FINISHED")==0;
        if (completed) {
            var matchDomain=matchRepository.findForUpdate(matchId).orElseThrow();
            var players=playerJpaRepository.findAllByMatchIdOrderByJoinedAt(matchId).stream()
                    .map(this::toDomain)
                    .sorted(Comparator.comparingInt(GameMatchPlayer::score).reversed()
                            .thenComparingLong(GameMatchPlayer::durationMilliseconds)
                            .thenComparing(playerResult->playerResult.userId().toString()))
                    .toList();
            for(int index=0;index<players.size();index++) {
                GameMatchPlayer ranked=players.get(index).withRank(index+1);
                GameMatchPlayerJpaEntity rankedEntity=playerJpaRepository.findById(ranked.id()).orElseThrow();
                rankedEntity.setRank(index+1); playerJpaRepository.save(rankedEntity);
            }
            matchDomain.complete(now); matchRepository.save(matchDomain);
        }
        return completed;
    }

    private GameMatchPlayer toDomain(GameMatchPlayerJpaEntity e) {
        return new GameMatchPlayer(e.getId(),e.getMatchId(),e.getRoomMemberId(),e.getUserId(),e.getScore(),e.getRank(),
                GameMatchPlayer.Status.valueOf(e.getStatus()),e.getJoinedAt(),e.getFinishedAt());
    }
}

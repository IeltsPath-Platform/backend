package com.ieltspath.game.application.usecase;

import com.ieltspath.game.application.port.WordlyRankingRepository;
import com.ieltspath.game.application.result.WordlyRankingResult;
import com.ieltspath.game.domain.exception.GameMatchNotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetWordlyRankingUseCase {
    private final WordlyRankingRepository rankings;

    public GetWordlyRankingUseCase(WordlyRankingRepository rankings) {
        this.rankings = rankings;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public WordlyRankingResult execute(UUID matchId, UUID userId) {
        return rankings.find(matchId, userId)
                .orElseThrow(() -> new GameMatchNotFoundException(matchId));
    }
}

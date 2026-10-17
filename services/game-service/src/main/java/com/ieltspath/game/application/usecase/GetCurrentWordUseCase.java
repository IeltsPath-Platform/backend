package com.ieltspath.game.application.usecase;
import com.ieltspath.game.application.port.WordlyMatchStore;
import com.ieltspath.game.application.result.CurrentWordResult;
import com.ieltspath.game.domain.exception.GameMatchNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
@Service
public class GetCurrentWordUseCase {
    private final WordlyMatchStore store;
    private final com.ieltspath.game.application.port.WordlyRankingRepository rankings;
    public GetCurrentWordUseCase(WordlyMatchStore store,com.ieltspath.game.application.port.WordlyRankingRepository rankings){this.store=store;this.rankings=rankings;}
    @Transactional(readOnly=true)
    public CurrentWordResult execute(UUID matchId,UUID userId){
        var current=store.current(matchId,userId);
        if(current.isPresent())return current.get();
        var ranking=rankings.find(matchId,userId).orElseThrow(()->new GameMatchNotFoundException(matchId));
        if("SOLO".equals(ranking.playerMode()))throw new GameMatchNotFoundException(matchId);
        var own=ranking.players().stream().filter(p->p.userId().equals(userId)).findFirst().orElseThrow();
        throw new com.ieltspath.game.domain.exception.SoloGameplayException(409,
            ranking.isFinal()?"MATCH_FINISHED":"PLAYER_NOT_ACTIVE","Player status: "+own.status());
    }
}

package com.group01.game.infrastructure.persistence.mapper;

import com.group01.game.domain.aggregate.GameMatchPlayer;
import com.group01.game.infrastructure.persistence.entity.GameMatchPlayerJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class GameMatchPlayerPersistenceMapper {
    public GameMatchPlayer toDomain(GameMatchPlayerJpaEntity entity) {
        return new GameMatchPlayer(entity.getId(), entity.getMatchId(), entity.getRoomMemberId(), entity.getUserId(),
                entity.getScore(), entity.getRank(), GameMatchPlayer.Status.valueOf(entity.getStatus()),
                entity.getJoinedAt(), entity.getFinishedAt());
    }

    public GameMatchPlayerJpaEntity toNewEntity(GameMatchPlayer player) {
        return new GameMatchPlayerJpaEntity(player.id(), player.matchId(), player.roomMemberId(),
                player.userId(), player.joinedAt());
    }
}

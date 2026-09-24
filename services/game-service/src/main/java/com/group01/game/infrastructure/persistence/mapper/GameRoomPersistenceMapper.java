package com.group01.game.infrastructure.persistence.mapper;

import com.group01.game.domain.aggregate.GameRoom;
import com.group01.game.infrastructure.persistence.entity.GameRoomJpaEntity;
import com.group01.game.infrastructure.persistence.entity.GameRoomMemberJpaEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GameRoomPersistenceMapper {
    public GameRoom toDomain(GameRoomJpaEntity entity, List<GameRoomMemberJpaEntity> memberEntities) {
        List<GameRoom.Member> members = memberEntities.stream()
                .map(member -> new GameRoom.Member(member.getId(), member.getUserId(),
                        GameRoom.MemberRole.valueOf(member.getMemberRole()),
                        GameRoom.MemberStatus.valueOf(member.getStatus()), member.getJoinedAt())).toList();
        return GameRoom.reconstitute(entity.getId(), entity.getRoomCode(), entity.getHostUserId(),
                entity.getMaxPlayers(), entity.getGameType(), entity.getLearningDomain(), entity.getMode(),
                entity.getConfigSnapshot(), entity.getCreatedAt(), entity.getUpdatedAt(), entity.getExpiresAt(),
                GameRoom.GameRoomStatus.valueOf(entity.getStatus()), members);
    }

    public GameRoomJpaEntity toNewEntity(GameRoom room) {
        return new GameRoomJpaEntity(room.id(), room.roomCode(), room.hostUserId(), room.gameType(),
                room.learningDomain(), room.mode(), room.maxPlayers(), room.configSnapshot(),
                room.createdAt(), room.expiresAt());
    }

    public GameRoomMemberJpaEntity toNewEntity(GameRoom room, GameRoom.Member member) {
        return new GameRoomMemberJpaEntity(member.id(), room.id(), member.userId(),
                member.role().name(), member.joinedAt());
    }
}

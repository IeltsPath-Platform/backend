package com.group01.game.infrastructure.persistence;

import com.group01.game.domain.aggregate.GameRoom;
import com.group01.game.domain.repository.GameRoomRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional
public class GameRoomRepositoryAdapter implements GameRoomRepository {
    private final GameRoomJpaRepository roomJpaRepository;
    private final GameRoomMemberJpaRepository memberJpaRepository;

    public GameRoomRepositoryAdapter(GameRoomJpaRepository roomJpaRepository,
                                     GameRoomMemberJpaRepository memberJpaRepository) {
        this.roomJpaRepository = roomJpaRepository;
        this.memberJpaRepository = memberJpaRepository;
    }

    @Override
    public GameRoom save(GameRoom room) {
        GameRoomJpaEntity entity = roomJpaRepository.findById(room.id()).orElseGet(() ->
                new GameRoomJpaEntity(room.id(), room.roomCode(), room.hostUserId(), room.gameType(),
                        room.learningDomain(), room.mode(), room.maxPlayers(), room.configSnapshot(),
                        room.createdAt(), room.expiresAt()));
        entity.setStatus(room.status().name(), room.updatedAt());
        roomJpaRepository.save(entity);
        Map<UUID, GameRoomMemberJpaEntity> existingByUser = new HashMap<>();
        memberJpaRepository.findAllByRoomIdOrderByJoinedAt(room.id())
                .forEach(member -> existingByUser.put(member.getUserId(), member));
        for (GameRoom.Member member : room.members().values()) {
            GameRoomMemberJpaEntity memberEntity = existingByUser.computeIfAbsent(member.userId(), ignored ->
                    new GameRoomMemberJpaEntity(member.id(), room.id(), member.userId(),
                            member.role().name(), member.joinedAt()));
            memberEntity.update(member.role().name(), member.status().name(), member.joinedAt(), room.updatedAt());
            memberJpaRepository.save(memberEntity);
        }
        return room;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GameRoom> findById(UUID id) {
        return roomJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<GameRoom> findForUpdate(UUID id) {
        return roomJpaRepository.findByIdForUpdate(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GameRoom> findByCode(String roomCode) {
        return roomJpaRepository.findByRoomCode(roomCode).map(this::toDomain);
    }

    private GameRoom toDomain(GameRoomJpaEntity entity) {
        List<GameRoom.Member> members = memberJpaRepository.findAllByRoomIdOrderByJoinedAt(entity.getId()).stream()
                .map(member -> new GameRoom.Member(member.getId(), member.getUserId(),
                        GameRoom.MemberRole.valueOf(member.getMemberRole()),
                        GameRoom.MemberStatus.valueOf(member.getStatus()), member.getJoinedAt())).toList();
        return GameRoom.reconstitute(entity.getId(), entity.getRoomCode(), entity.getHostUserId(),
                entity.getMaxPlayers(), entity.getGameType(), entity.getLearningDomain(), entity.getMode(),
                entity.getConfigSnapshot(), entity.getCreatedAt(), entity.getUpdatedAt(), entity.getExpiresAt(),
                GameRoom.GameRoomStatus.valueOf(entity.getStatus()), members);
    }
}

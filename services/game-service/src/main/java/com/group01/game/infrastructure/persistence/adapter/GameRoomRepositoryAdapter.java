package com.group01.game.infrastructure.persistence.adapter;

import com.group01.game.domain.aggregate.GameRoom;
import com.group01.game.domain.repository.GameRoomRepository;
import com.group01.game.infrastructure.persistence.entity.GameRoomJpaEntity;
import com.group01.game.infrastructure.persistence.entity.GameRoomMemberJpaEntity;
import com.group01.game.infrastructure.persistence.mapper.GameRoomPersistenceMapper;
import com.group01.game.infrastructure.persistence.repository.GameRoomJpaRepository;
import com.group01.game.infrastructure.persistence.repository.GameRoomMemberJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional
public class GameRoomRepositoryAdapter implements GameRoomRepository {
    private final GameRoomJpaRepository roomJpaRepository;
    private final GameRoomMemberJpaRepository memberJpaRepository;
    private final GameRoomPersistenceMapper mapper;

    public GameRoomRepositoryAdapter(GameRoomJpaRepository roomJpaRepository,
                                     GameRoomMemberJpaRepository memberJpaRepository,
                                     GameRoomPersistenceMapper mapper) {
        this.roomJpaRepository = roomJpaRepository;
        this.memberJpaRepository = memberJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public GameRoom save(GameRoom room) {
        GameRoomJpaEntity entity = roomJpaRepository.findById(room.id()).orElseGet(() -> mapper.toNewEntity(room));
        entity.setStatus(room.status().name(), room.updatedAt());
        roomJpaRepository.save(entity);
        Map<UUID, GameRoomMemberJpaEntity> existingByUser = new HashMap<>();
        memberJpaRepository.findAllByRoomIdOrderByJoinedAt(room.id())
                .forEach(member -> existingByUser.put(member.getUserId(), member));
        for (GameRoom.Member member : room.members().values()) {
            GameRoomMemberJpaEntity memberEntity = existingByUser.computeIfAbsent(member.userId(), ignored ->
                    mapper.toNewEntity(room, member));
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
        return mapper.toDomain(entity, memberJpaRepository.findAllByRoomIdOrderByJoinedAt(entity.getId()));
    }
}

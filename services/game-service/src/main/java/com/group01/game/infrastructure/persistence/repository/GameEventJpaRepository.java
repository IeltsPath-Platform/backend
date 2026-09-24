package com.group01.game.infrastructure.persistence.repository;

import com.group01.game.infrastructure.persistence.entity.GameEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface GameEventJpaRepository extends JpaRepository<GameEventJpaEntity, UUID> {
    @Query("select coalesce(max(event.sequenceNo), 0) from GameEventJpaEntity event where event.matchId = :matchId")
    long findMaxSequence(@Param("matchId") UUID matchId);
}

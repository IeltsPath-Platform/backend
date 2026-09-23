package com.group01.game.infrastructure.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface GameMatchPlayerJpaRepository extends JpaRepository<GameMatchPlayerJpaEntity, UUID> {
    List<GameMatchPlayerJpaEntity> findAllByMatchIdOrderByJoinedAt(UUID matchId);
    Optional<GameMatchPlayerJpaEntity> findByMatchIdAndUserId(UUID matchId, UUID userId);
    long countByMatchIdAndStatusNot(UUID matchId, String status);
}

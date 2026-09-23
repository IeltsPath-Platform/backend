package com.group01.game.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "game_match_players")
public class GameMatchPlayerJpaEntity {
    @Id private UUID id;
    @Column(name="match_id", nullable=false) private UUID matchId;
    @Column(name="room_member_id") private UUID roomMemberId;
    @Column(name="user_id", nullable=false) private UUID userId;
    @Column(nullable=false) private int score;
    @Column private Integer rank;
    @Column(nullable=false, length=30) private String status;
    @Column(name="joined_at", nullable=false) private Instant joinedAt;
    @Column(name="finished_at") private Instant finishedAt;
    protected GameMatchPlayerJpaEntity() {}
    public GameMatchPlayerJpaEntity(UUID id, UUID matchId, UUID roomMemberId, UUID userId, Instant joinedAt) { this.id=id; this.matchId=matchId; this.roomMemberId=roomMemberId; this.userId=userId; this.status="ACTIVE"; this.joinedAt=joinedAt; }
    public UUID getId() { return id; }
    public UUID getMatchId() { return matchId; }
    public UUID getRoomMemberId() { return roomMemberId; }
    public UUID getUserId() { return userId; }
    public int getScore() { return score; }
    public String getStatus() { return status; }
    public Instant getJoinedAt() { return joinedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public Integer getRank() { return rank; }
    public void updateProgress(int score, boolean finished, Instant at) { this.score=score; if (finished) { this.status="FINISHED"; this.finishedAt=at; } }
    public void setRank(int rank) { this.rank=rank; }
}

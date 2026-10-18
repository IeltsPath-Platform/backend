package com.ieltspath.game.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "game_room_members")
@SQLRestriction("is_delete = false")
public class GameRoomMemberJpaEntity {
    @Id
    private UUID id;
    @Column(name = "room_id", nullable = false)
    private UUID roomId;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(name = "member_role", nullable = false, length = 20)
    private String memberRole;
    @Column(nullable = false, length = 30)
    private String status;
    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;
    @Column(name = "ready_at")
    private Instant readyAt;
    @Column(name = "left_at")
    private Instant leftAt;
    @Column(name = "last_seen_at")
    private Instant lastSeenAt;
    @Column(name = "slot_number")
    private Short slotNumber;
    @Column(name = "display_name", length = 255)
    private String displayName;
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String value) { displayName = value; }
    @Column(name = "is_delete", nullable = false)
    private boolean isDelete;
    @Column(name = "deleted_at")
    private Instant deletedAt;

    public Integer getSlotNumber() { return slotNumber == null ? null : slotNumber.intValue(); }
    public void setSlotNumber(Integer slot) { slotNumber = slot == null ? null : slot.shortValue(); }

    protected GameRoomMemberJpaEntity() {
    }

    public GameRoomMemberJpaEntity(UUID id, UUID roomId, UUID userId, String role, Instant joinedAt) {
        this.id = id;
        this.roomId = roomId;
        this.userId = userId;
        this.memberRole = role;
        this.status = "JOINED";
        this.joinedAt = joinedAt;
        this.lastSeenAt = joinedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getRoomId() {
        return roomId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getMemberRole() {
        return memberRole;
    }

    public String getStatus() {
        return status;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public Instant getReadyAt() {
        return readyAt;
    }

    public void setStatus(String status, Instant at) {
        this.status = status;
        this.readyAt = "READY".equals(status) ? at : null;
        this.leftAt = "LEFT".equals(status) ? at : null;
        this.lastSeenAt = at;
    }

    public void update(String memberRole, String status, Instant joinedAt, Instant at) {
        this.memberRole = memberRole;
        this.joinedAt = joinedAt;
        setStatus(status, at);
    }
}

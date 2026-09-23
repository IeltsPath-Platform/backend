package com.group01.game.domain.aggregate;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class GameMatch {
    public enum Status { IN_PROGRESS, COMPLETED, CANCELLED }
    private final UUID id; private final UUID roomId; private final String gameType; private final String learningDomain;
    private final Map<String, Object> configSnapshot; private final Map<String, Object> contentSnapshot;
    private final Instant startedAt; private final Instant createdAt; private Status status; private Instant endedAt;
    public GameMatch(UUID id, UUID roomId, String gameType, String learningDomain, Map<String,Object> config, Map<String,Object> content, Instant now) {
        this(id,roomId,gameType,learningDomain,config,content,Status.IN_PROGRESS,now,null,now);
    }
    private GameMatch(UUID id, UUID roomId, String gameType, String learningDomain, Map<String,Object> config, Map<String,Object> content, Status status, Instant startedAt, Instant endedAt, Instant createdAt) {
        this.id=id; this.roomId=roomId; this.gameType=gameType; this.learningDomain=learningDomain; this.configSnapshot=Map.copyOf(config); this.contentSnapshot=Map.copyOf(content); this.status=status; this.startedAt=startedAt; this.endedAt=endedAt; this.createdAt=createdAt;
    }
    public static GameMatch reconstitute(UUID id, UUID roomId, String gameType, String learningDomain, Map<String,Object> config, Map<String,Object> content, Status status, Instant startedAt, Instant endedAt, Instant createdAt) {
        return new GameMatch(id,roomId,gameType,learningDomain,config,content,status,startedAt,endedAt,createdAt);
    }
    public void complete(Instant at) { if(status!=Status.IN_PROGRESS) throw new IllegalStateException("Match is not in progress"); status=Status.COMPLETED; endedAt=at; }
    public UUID id(){return id;} public UUID roomId(){return roomId;} public String gameType(){return gameType;} public String learningDomain(){return learningDomain;}
    public Map<String,Object> configSnapshot(){return configSnapshot;} public Map<String,Object> contentSnapshot(){return contentSnapshot;}
    public Status status(){return status;} public Instant startedAt(){return startedAt;} public Instant endedAt(){return endedAt;} public Instant createdAt(){return createdAt;}
}

package com.ieltspath.community.application.port;
import java.util.UUID;
public interface CommunityEventOutbox {void record(String eventType,UUID actorId,UUID recipientId,UUID postId,UUID commentId);}

package com.ieltspath.game.application.result;

import java.util.UUID;

public record RoomMemberKickedEvent(UUID roomId, UUID userId) {}

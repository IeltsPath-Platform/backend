package com.ieltspath.game.application.result;

import java.util.UUID;

public record WordlyMatchUpdatedEvent(UUID roomId, UUID matchId, UUID userId, boolean finished,
                                      String closureMessage) {}

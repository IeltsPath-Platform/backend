package com.ieltspath.game.application.result;
import java.util.UUID;
import java.util.Map;
public record SoloGameplayEvent(UUID roomId,UUID userId,Map<String,Object> payload){}

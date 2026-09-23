package com.group01.content.application.command;

import java.util.List;
import java.util.UUID;

public record GetGameContentSnapshotCommand(String gameType, String learningDomain, List<UUID> contentIds) {
    public GetGameContentSnapshotCommand { contentIds = List.copyOf(contentIds); }
}

package com.group01.content.api.controller;

import com.group01.content.api.dto.internal.GameContentSnapshotRequest;
import com.group01.content.api.dto.internal.GameContentSnapshotResponse;
import com.group01.content.application.command.GetGameContentSnapshotCommand;
import com.group01.content.application.usecase.GetGameContentSnapshotUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/game-content")
public class InternalGameContentController {
    private final GetGameContentSnapshotUseCase getGameContentSnapshot;

    public InternalGameContentController(GetGameContentSnapshotUseCase getGameContentSnapshot) {
        this.getGameContentSnapshot = getGameContentSnapshot;
    }

    @PostMapping("/snapshots")
    public GameContentSnapshotResponse snapshot(@Valid @RequestBody GameContentSnapshotRequest request) {
        return GameContentSnapshotResponse.from(getGameContentSnapshot.execute(
                new GetGameContentSnapshotCommand(request.gameType(), request.learningDomain(), request.contentIds())));
    }
}

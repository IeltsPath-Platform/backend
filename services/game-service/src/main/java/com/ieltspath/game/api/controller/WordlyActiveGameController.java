package com.ieltspath.game.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.game.application.result.ActiveWordlyGameResult;
import com.ieltspath.game.application.usecase.ActiveWordlyGameUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
/** One room per user across every room game; the Wordly path predates Rocket and stays as an alias. */
@RequestMapping({"/api/games/me/active-game", "/api/games/wordly/me/active-game"})
public class WordlyActiveGameController {
    private final CurrentUserProvider currentUser;
    private final ActiveWordlyGameUseCase activeGame;

    public WordlyActiveGameController(CurrentUserProvider currentUser, ActiveWordlyGameUseCase activeGame) {
        this.currentUser = currentUser;
        this.activeGame = activeGame;
    }

    /** 200 with the room the caller still belongs to, or 204 when they can create or join another. */
    @GetMapping
    public ResponseEntity<ActiveWordlyGameResult> get() {
        return activeGame.find(currentUser.requireUserId()).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/leave")
    public ResponseEntity<Void> leave() {
        activeGame.leave(currentUser.requireUserId());
        return ResponseEntity.noContent().build();
    }
}

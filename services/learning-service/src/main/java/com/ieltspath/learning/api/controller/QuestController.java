package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.application.usecase.GetQuestsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/learning/me/quests")
@RequiredArgsConstructor
public class QuestController {
    private final CurrentUserProvider currentUser;
    private final GetQuestsUseCase quests;

    /** Today's three quests with their progress, rewards and, for the weakness quest, the point to practise. */
    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public GetQuestsUseCase.QuestsView view() {
        return quests.view(currentUser.requireUserId());
    }
}

package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.application.usecase.BadgesUseCase;
import com.ieltspath.learning.domain.repository.BadgeRepository.Badge;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/learning")
@RequiredArgsConstructor
public class BadgeController {
    private final CurrentUserProvider currentUser;
    private final BadgesUseCase badges;

    /** The badges the learner holds and those still to earn, with progress where it can be counted. */
    @GetMapping("/me/badges")
    @PreAuthorize("hasRole('CUSTOMER')")
    public List<BadgesUseCase.Item> mine() {
        return badges.mine(currentUser.requireUserId());
    }

    /** Switches a badge on or off. A switched-off badge is no longer awarded; held badges are kept. */
    @PatchMapping("/admin/badges/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    public Badge setActive(@PathVariable("code") String code, @Valid @RequestBody ActiveRequest request) {
        return badges.setActive(code, request.active());
    }

    public record ActiveRequest(@NotNull Boolean active) {}
}

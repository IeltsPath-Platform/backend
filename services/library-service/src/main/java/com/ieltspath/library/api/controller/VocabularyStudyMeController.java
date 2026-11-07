package com.ieltspath.library.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.library.application.usecase.*;
import com.ieltspath.library.application.result.StudyGamificationResult.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController @RequiredArgsConstructor
@RequestMapping("/api/learning-support/vocabulary-study/me")
public class VocabularyStudyMeController {
    private final CurrentUserProvider currentUser;
    private final GetStudySummaryUseCase summary;
    private final UpdateStudySettingsUseCase settings;
    private final RefillStudyHeartsUseCase refill;
    private final BuyStudyFreezeUseCase freeze;
    public record SettingsRequest(@NotBlank String timezone) {}
    public record PurchaseRequest(@NotNull UUID requestId) {}
    @GetMapping("/summary") public Summary summary(){return summary.execute(currentUser.requireUserId());}
    @PutMapping("/settings") public Settings settings(@Valid @RequestBody SettingsRequest body){return settings.execute(currentUser.requireUserId(),body.timezone());}
    @PostMapping("/hearts/refill") public PurchaseResult refill(@Valid @RequestBody PurchaseRequest body){return refill.execute(currentUser.requireUserId(),body.requestId());}
    @PostMapping("/streak/freeze") public PurchaseResult freeze(@Valid @RequestBody PurchaseRequest body){return freeze.execute(currentUser.requireUserId(),body.requestId());}
}

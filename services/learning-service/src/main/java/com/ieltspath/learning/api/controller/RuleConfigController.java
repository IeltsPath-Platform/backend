package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.api.dto.request.UpdateRuleConfigRequest;
import com.ieltspath.learning.application.result.RuleConfigResult;
import com.ieltspath.learning.application.usecase.ManageRuleConfigUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/learning/admin/rule-configs")
@PreAuthorize("hasRole('ADMIN')")
public class RuleConfigController {
    private final CurrentUserProvider currentUser;
    private final ManageRuleConfigUseCase rules;

    public RuleConfigController(CurrentUserProvider currentUser, ManageRuleConfigUseCase rules) {
        this.currentUser = currentUser;
        this.rules = rules;
    }

    @GetMapping
    public RuleConfigResult get() {
        return rules.get();
    }

    @PutMapping
    public RuleConfigResult update(@Valid @RequestBody UpdateRuleConfigRequest request) {
        return rules.update(currentUser.requireUserId(), request.toCommand());
    }

    /** 202 when the re-tiering started, 409 when another maintenance job is still running. */
    @PostMapping("/recompute")
    public ResponseEntity<Map<String, String>> recompute() {
        return rules.recompute()
                ? ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("status", "STARTED"))
                : ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("status", "ALREADY_RUNNING"));
    }
}

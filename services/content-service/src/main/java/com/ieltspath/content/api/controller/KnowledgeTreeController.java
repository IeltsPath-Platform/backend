package com.ieltspath.content.api.controller;

import com.ieltspath.content.api.dto.response.KnowledgeTreeHealthResponse;
import com.ieltspath.content.api.dto.response.KnowledgeTreeResponse;
import com.ieltspath.content.application.usecase.GetKnowledgeTreeHealthUseCase;
import com.ieltspath.content.application.usecase.GetKnowledgeTreeUseCase;
import com.ieltspath.content.domain.vo.Skill;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/content/knowledge-tree")
@RequiredArgsConstructor
public class KnowledgeTreeController {
    private final GetKnowledgeTreeUseCase getKnowledgeTreeUseCase;
    private final GetKnowledgeTreeHealthUseCase getKnowledgeTreeHealthUseCase;

    @GetMapping("/health")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHOR', 'EXAMINER')")
    public KnowledgeTreeHealthResponse getKnowledgeTreeHealth() {
        return KnowledgeTreeHealthResponse.from(getKnowledgeTreeHealthUseCase.execute());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CONTENT_AUTHOR', 'EXAMINER')")
    public KnowledgeTreeResponse getKnowledgeTree(
            @RequestParam(value = "courseId", required = false) UUID courseId,
            @RequestParam(value = "skill", required = false) Skill skill,
            @RequestParam(value = "includeInactive", defaultValue = "false") boolean includeInactive,
            Authentication authentication) {
        boolean canViewInactive = includeInactive && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_CONTENT_AUTHOR"));
        return KnowledgeTreeResponse.from(getKnowledgeTreeUseCase.execute(courseId, skill, canViewInactive));
    }
}

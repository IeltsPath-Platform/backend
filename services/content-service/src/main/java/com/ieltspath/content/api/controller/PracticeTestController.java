package com.ieltspath.content.api.controller;

import com.ieltspath.content.api.dto.request.SearchPracticeTestsRequest;
import com.ieltspath.content.api.dto.response.PracticeTestCardResponse;
import com.ieltspath.content.api.dto.response.PracticeTestPageResponse;
import com.ieltspath.content.api.dto.response.SpeakingTaskResponse;
import com.ieltspath.content.api.dto.response.WritingTaskResponse;
import com.ieltspath.content.application.command.SearchPracticeTestsCommand;
import com.ieltspath.content.application.port.PracticeTestCatalogReader;
import com.ieltspath.content.application.usecase.GetSpeakingTaskUseCase;
import com.ieltspath.content.application.usecase.GetWritingTaskUseCase;
import com.ieltspath.content.application.usecase.ListPracticeTestFormatsUseCase;
import com.ieltspath.content.application.usecase.ListTestSourcesUseCase;
import com.ieltspath.content.application.usecase.SearchPracticeTestsUseCase;
import com.ieltspath.content.domain.vo.Skill;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/content/practice-tests")
@RequiredArgsConstructor
public class PracticeTestController {
    private final SearchPracticeTestsUseCase search;
    private final ListTestSourcesUseCase sources;
    private final ListPracticeTestFormatsUseCase formats;
    private final GetWritingTaskUseCase writingTask;
    private final GetSpeakingTaskUseCase speakingTask;

    @PostMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_AUTHOR', 'CUSTOMER', 'EXAMINER')")
    public PracticeTestPageResponse search(@Valid @RequestBody SearchPracticeTestsRequest request) {
        Skill skill;
        try { skill = Skill.valueOf(request.skill()); }
        catch (RuntimeException invalid) { throw new IllegalArgumentException("Unknown skill"); }
        var command = new SearchPracticeTestsCommand(skill, request.scope(), request.partNo(), request.sources(),
                request.query(), map(request.only()), map(request.exclude()), request.page() == null ? 0 : request.page(),
                request.size() == null ? 12 : request.size(),
                request.formats() == null ? List.of() : request.formats());
        PracticeTestCatalogReader.Page page = search.execute(command);
        return new PracticeTestPageResponse(page.totalElements(), command.page(), command.size(), page.items().stream()
                .map(card -> PracticeTestCardResponse.from(card, search.coverUrl(card))).toList());
    }

    @GetMapping("/sources")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_AUTHOR', 'CUSTOMER', 'EXAMINER')")
    public List<ListTestSourcesUseCase.Source> sources() { return sources.execute(); }

    @GetMapping("/formats")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_AUTHOR', 'CUSTOMER', 'EXAMINER')")
    public List<PracticeTestCatalogReader.FormatCount> formats(@RequestParam("skill") String skill) {
        Skill parsed;
        try { parsed = Skill.valueOf(skill); }
        catch (RuntimeException invalid) { throw new IllegalArgumentException("Unknown skill"); }
        return formats.execute(parsed);
    }

    /** The prompt, chart and writing guide of one Writing task, for the free-writing and step-by-step workspaces. */
    @GetMapping("/{packageVersionId}/sections/{sectionId}/writing-task")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_AUTHOR', 'CUSTOMER', 'EXAMINER')")
    public WritingTaskResponse writingTask(@PathVariable("packageVersionId") UUID packageVersionId,
                                           @PathVariable("sectionId") UUID sectionId) {
        return WritingTaskResponse.from(writingTask.execute(packageVersionId, sectionId));
    }

    /** The questions of one Speaking part (Part 1, 2 or 3), for the recording workspace. */
    @GetMapping("/{packageVersionId}/sections/{sectionId}/speaking-task")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_AUTHOR', 'CUSTOMER', 'EXAMINER')")
    public SpeakingTaskResponse speakingTask(@PathVariable("packageVersionId") UUID packageVersionId,
                                             @PathVariable("sectionId") UUID sectionId) {
        return SpeakingTaskResponse.from(speakingTask.execute(packageVersionId, sectionId));
    }

    private static List<PracticeTestCatalogReader.Key> map(List<SearchPracticeTestsRequest.Key> keys) {
        return keys == null ? List.of() : keys.stream()
                .map(key -> new PracticeTestCatalogReader.Key(key.packageId(), key.partNo())).toList();
    }
}

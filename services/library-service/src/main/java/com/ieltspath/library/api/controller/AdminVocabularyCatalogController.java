package com.ieltspath.library.api.controller;

import com.ieltspath.library.api.dto.request.CollocationRequest;
import com.ieltspath.library.api.dto.request.CommonMistakeRequest;
import com.ieltspath.library.api.dto.request.UpdateVocabularyItemRequest;
import com.ieltspath.library.api.dto.request.UpdateVocabularySenseRequest;
import com.ieltspath.library.api.dto.request.UsageExampleRequest;
import com.ieltspath.library.api.dto.request.VocabularySetRequest;
import com.ieltspath.library.api.dto.request.VocabularyTopicRequest;
import com.ieltspath.library.api.dto.request.WordRelationRequest;
import com.ieltspath.library.application.result.CatalogItemResult;
import com.ieltspath.library.application.result.CollocationResult;
import com.ieltspath.library.application.result.CommonMistakeResult;
import com.ieltspath.library.application.result.UsageExampleResult;
import com.ieltspath.library.application.result.VocabularySetResult;
import com.ieltspath.library.application.result.VocabularyTopicResult;
import com.ieltspath.library.application.result.WordRelationResult;
import com.ieltspath.library.application.usecase.VocabularyCatalogUseCase;
import com.ieltspath.library.domain.catalog.CatalogLimits;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Author edits of the dictionary catalog. Rules live in the domain values the requests are mapped to. */
@RestController
@RequestMapping("/api/content/admin/vocabulary")
@PreAuthorize("hasAnyRole('CONTENT_AUTHOR','ADMIN')")
@RequiredArgsConstructor
@Validated
public class AdminVocabularyCatalogController {
    private final VocabularyCatalogUseCase catalog;

    @PutMapping("/items/{id}")
    public CatalogItemResult updateItem(@PathVariable UUID id, @Valid @RequestBody UpdateVocabularyItemRequest request) {
        return catalog.updateItem(id, request.toDetails());
    }

    @PutMapping("/senses/{id}")
    public CatalogItemResult updateSense(@PathVariable UUID id, @Valid @RequestBody UpdateVocabularySenseRequest request) {
        return catalog.updateSense(id, request.toDetails());
    }

    @PutMapping("/senses/{id}/collocations")
    public List<CollocationResult> replaceCollocations(
            @PathVariable UUID id,
            @RequestBody @NotNull @Size(max = CatalogLimits.MAX_CHILDREN_PER_SENSE) List<@Valid @NotNull CollocationRequest> body) {
        return catalog.replaceCollocations(id, body.stream().map(CollocationRequest::toCollocation).toList());
    }

    @PutMapping("/senses/{id}/relations")
    public List<WordRelationResult> replaceRelations(
            @PathVariable UUID id,
            @RequestBody @NotNull @Size(max = CatalogLimits.MAX_CHILDREN_PER_SENSE) List<@Valid @NotNull WordRelationRequest> body) {
        return catalog.replaceRelations(id, body.stream().map(WordRelationRequest::toRelation).toList());
    }

    @PutMapping("/senses/{id}/common-mistakes")
    public List<CommonMistakeResult> replaceCommonMistakes(
            @PathVariable UUID id,
            @RequestBody @NotNull @Size(max = CatalogLimits.MAX_CHILDREN_PER_SENSE) List<@Valid @NotNull CommonMistakeRequest> body) {
        return catalog.replaceCommonMistakes(id, body.stream().map(CommonMistakeRequest::toMistake).toList());
    }

    @PutMapping("/senses/{id}/examples")
    public List<UsageExampleResult> replaceExamples(
            @PathVariable UUID id,
            @RequestBody @NotNull @Size(max = CatalogLimits.MAX_CHILDREN_PER_SENSE) List<@Valid @NotNull UsageExampleRequest> body) {
        return catalog.replaceExamples(id, body.stream().map(UsageExampleRequest::toExample).toList());
    }

    @GetMapping("/topics")
    public List<VocabularyTopicResult> topics() {
        return catalog.allTopics();
    }

    @PostMapping("/topics")
    @ResponseStatus(HttpStatus.CREATED)
    public VocabularyTopicResult createTopic(@Valid @RequestBody VocabularyTopicRequest request) {
        return catalog.createTopic(request.toDraft());
    }

    @PutMapping("/topics/{id}")
    public VocabularyTopicResult updateTopic(@PathVariable UUID id, @Valid @RequestBody VocabularyTopicRequest request) {
        return catalog.updateTopic(id, request.toDraft());
    }

    @DeleteMapping("/topics/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTopic(@PathVariable UUID id) {
        catalog.deactivateTopic(id);
    }

    @GetMapping("/sets")
    public List<VocabularySetResult> sets() {
        return catalog.allSets();
    }

    @PostMapping("/sets")
    @ResponseStatus(HttpStatus.CREATED)
    public VocabularySetResult createSet(@Valid @RequestBody VocabularySetRequest request) {
        return catalog.createSet(request.toDraft());
    }

    @PutMapping("/sets/{id}")
    public VocabularySetResult updateSet(@PathVariable UUID id, @Valid @RequestBody VocabularySetRequest request) {
        return catalog.updateSet(id, request.toDraft());
    }

    @DeleteMapping("/sets/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSet(@PathVariable UUID id) {
        catalog.deactivateSet(id);
    }

    @PutMapping("/sets/{id}/items")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void replaceSetItems(@PathVariable UUID id,
                                @RequestBody @NotNull @Size(max = CatalogLimits.MAX_SENSES_PER_SET) List<@NotNull UUID> senseIds) {
        catalog.replaceSetItems(id, senseIds);
    }
}

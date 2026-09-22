package com.group01.content.api.controller;

import com.group01.content.api.dto.request.AddVocabularySenseRequest;
import com.group01.content.api.dto.request.CreateVocabularyItemRequest;
import com.group01.content.api.dto.response.VocabularyItemResponse;
import com.group01.content.api.dto.response.VocabularySenseResponse;
import com.group01.content.application.command.AddVocabularySenseCommand;
import com.group01.content.application.command.CreateVocabularyItemCommand;
import com.group01.content.application.result.VocabularyItemResult;
import com.group01.content.application.result.VocabularySenseResult;
import com.group01.content.application.usecase.AddVocabularySenseUseCase;
import com.group01.content.application.usecase.CreateVocabularyItemUseCase;
import com.group01.content.application.usecase.GetVocabularyDetailUseCase;
import com.group01.content.application.usecase.SearchVocabularyUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/content/vocabulary", "/api/content/admin/vocabulary"})
@RequiredArgsConstructor
public class VocabularyController {

    private final SearchVocabularyUseCase searchVocabularyUseCase;
    private final GetVocabularyDetailUseCase getVocabularyDetailUseCase;
    private final CreateVocabularyItemUseCase createVocabularyItemUseCase;
    private final AddVocabularySenseUseCase addVocabularySenseUseCase;

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_AUTHOR', 'CUSTOMER', 'EXAMINER')")
    public List<VocabularyItemResponse> search(@RequestParam(value = "query", defaultValue = "") String query) {
        return searchVocabularyUseCase.execute(query).stream()
                .map(VocabularyItemResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_AUTHOR', 'CUSTOMER', 'EXAMINER')")
    public VocabularyItemResponse getById(@PathVariable("id") UUID id) {
        return VocabularyItemResponse.from(getVocabularyDetailUseCase.execute(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_AUTHOR')")
    public VocabularyItemResponse create(@Valid @RequestBody CreateVocabularyItemRequest request) {
        VocabularyItemResult result = createVocabularyItemUseCase.execute(new CreateVocabularyItemCommand(
                request.lemma(),
                request.ipa(),
                request.pronunciationAudioReference()
        ));
        return VocabularyItemResponse.from(result);
    }

    @PostMapping("/{id}/senses")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_AUTHOR')")
    public VocabularyItemResponse addSense(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AddVocabularySenseRequest request
    ) {
        VocabularyItemResult result = addVocabularySenseUseCase.execute(new AddVocabularySenseCommand(
                id,
                request.partOfSpeech(),
                request.englishDefinition(),
                request.vietnameseMeaning(),
                request.exampleSentence(),
                request.imageUrl(),
                request.sortOrder()
        ));
        return VocabularyItemResponse.from(result);
    }
}

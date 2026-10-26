package com.ieltspath.library.api.controller;

import com.ieltspath.library.api.dto.request.AddVocabularySenseRequest;
import com.ieltspath.library.api.dto.request.CreateVocabularyItemRequest;
import com.ieltspath.library.api.dto.response.VocabularyItemResponse;
import com.ieltspath.library.api.dto.response.VocabularySenseCardResponse;
import com.ieltspath.library.application.command.AddVocabularySenseCommand;
import com.ieltspath.library.application.command.CreateVocabularyItemCommand;
import com.ieltspath.library.application.usecase.AddVocabularySenseUseCase;
import com.ieltspath.library.application.usecase.CreateVocabularyItemUseCase;
import com.ieltspath.library.application.usecase.GetVocabularyDetailUseCase;
import com.ieltspath.library.application.usecase.GetVocabularySensesUseCase;
import com.ieltspath.library.application.usecase.SearchVocabularyUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.List;
import java.util.Arrays;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/content/admin/vocabulary")
@RequiredArgsConstructor
public class AdminVocabularyController {
    private final CreateVocabularyItemUseCase create;
    private final AddVocabularySenseUseCase addSense;
    private final GetVocabularyDetailUseCase getDetail;
    private final SearchVocabularyUseCase search;
    private final GetVocabularySensesUseCase senses;
    private static final Pattern UUID_PATTERN = Pattern.compile("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHOR', 'CUSTOMER', 'EXAMINER')")
    public List<VocabularyItemResponse> search(@RequestParam(value = "query", defaultValue = "") String query) {
        return search.execute(query).stream().map(VocabularyItemResponse::from).toList();
    }

    @GetMapping("/senses")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHOR', 'CUSTOMER', 'EXAMINER')")
    public List<VocabularySenseCardResponse> getSenses(@RequestParam("ids") String ids) {
        if (ids == null || ids.isBlank()) throw new IllegalArgumentException("ids must contain UUIDs");
        String[] values = ids.split(",", -1);
        if (values.length > 100 || Arrays.stream(values).anyMatch(value -> !UUID_PATTERN.matcher(value).matches()))
            throw new IllegalArgumentException("ids must contain up to 100 UUIDs");
        return senses.execute(Arrays.stream(values).map(UUID::fromString).toList()).stream()
                .map(VocabularySenseCardResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHOR', 'CUSTOMER', 'EXAMINER')")
    public VocabularyItemResponse getById(@PathVariable UUID id) {
        return VocabularyItemResponse.from(getDetail.execute(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CONTENT_AUTHOR')")
    public VocabularyItemResponse create(@Valid @RequestBody CreateVocabularyItemRequest request) {
        return VocabularyItemResponse.from(create.execute(new CreateVocabularyItemCommand(
                request.lemma(), request.ipa(), request.pronunciationAudioReference())));
    }

    @PostMapping("/{id}/senses")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CONTENT_AUTHOR')")
    public VocabularyItemResponse addSense(@PathVariable UUID id, @Valid @RequestBody AddVocabularySenseRequest request) {
        return VocabularyItemResponse.from(addSense.execute(new AddVocabularySenseCommand(id, request.partOfSpeech(),
                request.englishDefinition(), request.vietnameseMeaning(), request.exampleSentence(), request.imageUrl(), request.sortOrder())));
    }
}

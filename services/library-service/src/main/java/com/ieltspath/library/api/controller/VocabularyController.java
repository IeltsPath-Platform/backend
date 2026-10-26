package com.ieltspath.library.api.controller;

import com.ieltspath.library.api.dto.request.AddVocabularySenseRequest;
import com.ieltspath.library.api.dto.request.CreateVocabularyItemRequest;
import com.ieltspath.library.api.dto.response.VocabularyItemResponse;
import com.ieltspath.library.api.dto.response.VocabularySenseResponse;
import com.ieltspath.library.api.dto.response.VocabularySenseCardResponse;
import com.ieltspath.library.application.command.AddVocabularySenseCommand;
import com.ieltspath.library.application.command.CreateVocabularyItemCommand;
import com.ieltspath.library.application.result.VocabularyItemResult;
import com.ieltspath.library.application.result.VocabularySenseResult;
import com.ieltspath.library.domain.vo.ContentStatus;
import com.ieltspath.library.domain.exception.ResourceNotFoundException;
import com.ieltspath.library.application.usecase.AddVocabularySenseUseCase;
import com.ieltspath.library.application.usecase.CreateVocabularyItemUseCase;
import com.ieltspath.library.application.usecase.GetVocabularyDetailUseCase;
import com.ieltspath.library.application.usecase.GetVocabularySensesUseCase;
import com.ieltspath.library.application.usecase.SearchVocabularyUseCase;
import com.ieltspath.library.application.usecase.BrowseVocabularyUseCase;
import com.ieltspath.library.application.usecase.VocabularyCatalogUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import com.ieltspath.library.api.dto.response.PageResponse;
import com.ieltspath.library.application.result.VocabularyItemResult;
import com.ieltspath.library.application.result.VocabularySenseResult;

import java.util.List;
import java.util.regex.Pattern;
import java.util.UUID;
import java.util.Map;

@RestController
@RequestMapping("/api/content/vocabulary")
@RequiredArgsConstructor
@Validated
public class VocabularyController {

    private static final Pattern UUID_PATTERN = Pattern.compile("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    private final SearchVocabularyUseCase searchVocabularyUseCase;
    private final GetVocabularyDetailUseCase getVocabularyDetailUseCase;
    private final GetVocabularySensesUseCase getVocabularySensesUseCase;
    private final CreateVocabularyItemUseCase createVocabularyItemUseCase;
    private final AddVocabularySenseUseCase addVocabularySenseUseCase;
    private final BrowseVocabularyUseCase browseVocabularyUseCase;
    private final VocabularyCatalogUseCase catalog;

    @GetMapping
    public ResponseEntity<Map<String,Object>> browse(
            @RequestParam(defaultValue = "") @Size(max = 100) String q,
            @RequestParam(defaultValue = "EN") @jakarta.validation.constraints.Pattern(regexp = "EN|VI", flags = jakarta.validation.constraints.Pattern.Flag.CASE_INSENSITIVE) String lang,
            @RequestParam(required = false) @jakarta.validation.constraints.Pattern(regexp = "A1|A2|B1|B2|C1|C2", flags = jakarta.validation.constraints.Pattern.Flag.CASE_INSENSITIVE) String cefr,
            @RequestParam(required = false) UUID topicId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        var response = catalog.browse(q, lang, cefr == null ? null : cefr.toUpperCase(), topicId, page, size);
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(java.time.Duration.ofMinutes(5)).cachePublic()).body(response);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHOR', 'CUSTOMER', 'EXAMINER')")
    public List<VocabularyItemResponse> search(@RequestParam(value = "query", defaultValue = "") String query) {
        return searchVocabularyUseCase.execute(query).stream()
                .map(VocabularyItemResponse::from)
                .toList();
    }

    @GetMapping("/senses")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHOR', 'CUSTOMER', 'EXAMINER')")
    public List<VocabularySenseCardResponse> getSenses(@RequestParam("ids") String ids) {
        return getVocabularySensesUseCase.execute(parseSenseIds(ids)).stream()
                .map(VocabularySenseCardResponse::from)
                .toList();
    }

    private List<UUID> parseSenseIds(String ids) {
        if (ids == null || ids.isBlank()) {
            throw new IllegalArgumentException("ids must contain between 1 and 100 UUIDs");
        }

        String[] values = ids.split(",", -1);
        if (values.length == 0 || values.length > 100) {
            throw new IllegalArgumentException("ids must contain between 1 and 100 UUIDs");
        }

        return java.util.Arrays.stream(values)
                .map(value -> {
                    if (!UUID_PATTERN.matcher(value).matches()) {
                        throw new IllegalArgumentException("ids must contain valid UUIDs");
                    }
                    return UUID.fromString(value);
                })
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String,Object>> getById(@PathVariable("id") UUID id) {
        var active = catalog.detail(id);
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(java.time.Duration.ofMinutes(5)).cachePublic())
                .body(active);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CONTENT_AUTHOR')")
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
    @PreAuthorize("hasRole('CONTENT_AUTHOR')")
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

    private static VocabularyItemResponse toResponse(com.ieltspath.library.domain.aggregate.VocabularyItem item) {
        return VocabularyItemResponse.from(new VocabularyItemResult(item.getId(), item.getLemma(), item.getNormalizedLemma(),
                item.getIpa(), item.getPronunciationAudioReference(), item.getStatus(), item.getCreatedAt(), item.getUpdatedAt(),
                item.getSenses().stream().filter(s -> s.getStatus() == ContentStatus.ACTIVE).map(s -> new VocabularySenseResult(s.getId(), s.getVocabularyItemId(), s.getPartOfSpeech(),
                        s.getEnglishDefinition(), s.getVietnameseMeaning(), s.getExampleSentence(), s.getImageUrl(), s.getSortOrder(),
                        s.getStatus(), s.getCreatedAt(), s.getUpdatedAt())).toList()));
    }
}

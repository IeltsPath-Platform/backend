package com.ieltspath.library.api.controller;

import com.ieltspath.library.application.query.PageQuery;
import com.ieltspath.library.application.result.CatalogItemResult;
import com.ieltspath.library.application.result.PageResult;
import com.ieltspath.library.application.result.VocabularyMindmapResult;
import com.ieltspath.library.application.result.VocabularySetResult;
import com.ieltspath.library.application.result.VocabularySuggestionResult;
import com.ieltspath.library.application.result.VocabularyTopicResult;
import com.ieltspath.library.application.usecase.VocabularyCatalogUseCase;
import com.ieltspath.library.domain.vo.VocabularySetKind;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/** Public dictionary reads (topics, sets, suggestions, mindmap); guests may call them, so responses carry no user data. */
@RestController
@RequestMapping("/api/content/vocabulary")
@RequiredArgsConstructor
@Validated
public class VocabularyCatalogController {
    private final VocabularyCatalogUseCase catalog;

    @GetMapping("/suggestions")
    public ResponseEntity<List<VocabularySuggestionResult>> suggestions(@RequestParam @Size(min = 1, max = 50) String q) {
        return publicResponse(catalog.suggestions(q));
    }

    @GetMapping("/topics")
    public ResponseEntity<List<VocabularyTopicResult>> topics() {
        return publicResponse(catalog.topics());
    }

    @GetMapping("/sets")
    public ResponseEntity<PageResult<VocabularySetResult>> sets(
            @RequestParam(required = false) VocabularySetKind kind,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return publicResponse(catalog.sets(kind, new PageQuery(page, size)));
    }

    @GetMapping("/sets/{id}")
    public ResponseEntity<VocabularySetResult> set(@PathVariable UUID id) {
        return publicResponse(catalog.set(id));
    }

    @GetMapping("/sets/{id}/words")
    public ResponseEntity<PageResult<CatalogItemResult>> setWords(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID topicId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return publicResponse(catalog.setWords(id, topicId, new PageQuery(page, size)));
    }

    @GetMapping("/mindmap")
    public ResponseEntity<VocabularyMindmapResult> mindmap(@RequestParam(required = false) UUID topicId,
                                                           @RequestParam(required = false) UUID itemId) {
        return publicResponse(catalog.mindmap(topicId, itemId));
    }

    private static <T> ResponseEntity<T> publicResponse(T body) {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic()).body(body);
    }
}

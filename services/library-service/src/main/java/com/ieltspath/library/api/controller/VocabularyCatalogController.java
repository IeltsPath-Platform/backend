package com.ieltspath.library.api.controller;

import com.ieltspath.library.application.usecase.VocabularyCatalogUseCase;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/content/vocabulary")
@RequiredArgsConstructor
@Validated
public class VocabularyCatalogController {
    private final VocabularyCatalogUseCase catalog;

    @GetMapping("/suggestions")
    public ResponseEntity<?> suggestions(@RequestParam @Size(min=1,max=50) String q) {
        return publicResponse(catalog.suggestions(q));
    }
    @GetMapping("/topics") public ResponseEntity<?> topics() { return publicResponse(catalog.topics()); }
    @GetMapping("/sets")
    public ResponseEntity<?> sets(@RequestParam(required=false) @jakarta.validation.constraints.Pattern(regexp="BAND|TOPIC",flags=jakarta.validation.constraints.Pattern.Flag.CASE_INSENSITIVE) String kind,
                                  @RequestParam(defaultValue="0") @Min(0) int page,
                                  @RequestParam(defaultValue="20") @Min(1) @Max(50) int size) {
        return publicResponse(catalog.sets(kind==null?null:kind.toUpperCase(),page,size));
    }
    @GetMapping("/sets/{id}") public ResponseEntity<?> set(@PathVariable UUID id) { return publicResponse(catalog.set(id)); }
    @GetMapping("/sets/{id}/words")
    public ResponseEntity<?> setWords(@PathVariable UUID id,@RequestParam(required=false) UUID topicId,
                                      @RequestParam(defaultValue="0") @Min(0) int page,
                                      @RequestParam(defaultValue="20") @Min(1) @Max(50) int size) {
        return publicResponse(catalog.setWords(id,topicId,page,size));
    }
    @GetMapping("/mindmap")
    public ResponseEntity<?> mindmap(@RequestParam(required=false) UUID topicId,@RequestParam(required=false) UUID itemId) {
        return publicResponse(catalog.mindmap(topicId,itemId));
    }
    private ResponseEntity<?> publicResponse(Object body) { return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic()).body(body); }
}

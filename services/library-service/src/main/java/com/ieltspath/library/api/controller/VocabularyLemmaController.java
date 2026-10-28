package com.ieltspath.library.api.controller;

import com.ieltspath.library.api.dto.request.BulkVocabularyRequest;
import com.ieltspath.library.api.dto.response.ResolvedVocabularyResponse;
import com.ieltspath.library.api.dto.response.VocabularyItemResponse;
import com.ieltspath.library.application.command.ResolveVocabularyCommand;
import com.ieltspath.library.application.usecase.FindVocabularyByLemmasUseCase;
import com.ieltspath.library.application.usecase.ResolveVocabularyUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/** Looks words up by exact lemma and gives reviewed words a sense, for authors building lessons in bulk. */
@RestController
@RequestMapping({"/api/content/vocabulary", "/api/content/admin/vocabulary"})
@RequiredArgsConstructor
public class VocabularyLemmaController {

    static final int MAX_LEMMAS = 100;

    private final FindVocabularyByLemmasUseCase findByLemmas;
    private final ResolveVocabularyUseCase resolve;

    @GetMapping("/by-lemmas")
    @PreAuthorize("hasRole('CONTENT_AUTHOR')")
    public List<VocabularyItemResponse> byLemmas(@RequestParam("lemmas") String lemmas) {
        List<String> names = Arrays.stream(lemmas.split(",", -1)).map(String::trim).filter(name -> !name.isEmpty())
                .toList();
        if (names.isEmpty() || names.size() > MAX_LEMMAS || names.stream().anyMatch(name -> name.length() > 255)) {
            throw new IllegalArgumentException("lemmas must contain between 1 and " + MAX_LEMMAS + " words");
        }
        return findByLemmas.execute(names).stream().map(VocabularyItemResponse::from).toList();
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasRole('CONTENT_AUTHOR')")
    public List<ResolvedVocabularyResponse> bulk(@Valid @RequestBody BulkVocabularyRequest request) {
        List<ResolveVocabularyCommand.Entry> entries = request.entries().stream()
                .map(entry -> new ResolveVocabularyCommand.Entry(entry.lemma(), entry.ipa(), entry.partOfSpeech(),
                        entry.englishDefinition(), entry.vietnameseMeaning(), entry.exampleSentence()))
                .toList();
        return resolve.execute(new ResolveVocabularyCommand(entries)).stream()
                .map(ResolvedVocabularyResponse::from).toList();
    }
}

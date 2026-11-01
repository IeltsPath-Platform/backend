package com.ieltspath.library.api.controller;

import com.ieltspath.library.api.dto.response.WordLookupResponse;
import com.ieltspath.library.application.usecase.LookupVocabularyWordUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/** The dictionary a learner opens by clicking a word of a reading passage. */
@RestController
@RequestMapping("/api/content/vocabulary")
@RequiredArgsConstructor
public class VocabularyLookupController {

    static final int MAX_FORMS = 10;
    static final int MAX_FORM_LENGTH = 64;

    private final LookupVocabularyWordUseCase lookup;

    /**
     * The words whose lemma is exactly one of the forms asked for (a learner sends the word as written and its likely
     * dictionary forms), in that order. A word without a sense still comes back with its pronunciation.
     */
    @GetMapping("/lookup")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHOR', 'CUSTOMER', 'EXAMINER')")
    public List<WordLookupResponse> lookup(@RequestParam("forms") String forms) {
        List<String> names = Arrays.stream(forms.split(",", -1)).map(String::trim).filter(name -> !name.isEmpty()).toList();
        if (names.isEmpty() || names.size() > MAX_FORMS || names.stream().anyMatch(name -> name.length() > MAX_FORM_LENGTH)) {
            throw new IllegalArgumentException("forms must contain between 1 and " + MAX_FORMS + " words");
        }
        return lookup.execute(names).stream().map(WordLookupResponse::from).toList();
    }
}

package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.command.ResolveVocabularyCommand;
import com.ieltspath.library.application.command.ResolveVocabularyCommand.Entry;
import com.ieltspath.library.application.result.ResolvedVocabularyResult;
import com.ieltspath.library.domain.aggregate.VocabularyItem;
import com.ieltspath.library.domain.entity.VocabularySense;
import com.ieltspath.library.domain.exception.InvalidContentStateException;
import com.ieltspath.library.domain.repository.VocabularyRepository;
import com.ieltspath.library.domain.vo.ContentStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Gives every reviewed word a sense. A lemma the library already has keeps its own first active sense and the entry's
 * text is ignored; a lemma it lacks is created with the sense the author reviewed. All of it is one transaction, and a
 * lemma that appears twice is resolved once.
 */
@Service
@Transactional
public class ResolveVocabularyUseCase {

    private final VocabularyRepository vocabulary;

    public ResolveVocabularyUseCase(VocabularyRepository vocabulary) {
        this.vocabulary = vocabulary;
    }

    public List<ResolvedVocabularyResult> execute(ResolveVocabularyCommand command) {
        Map<String, VocabularyItem> known = new HashMap<>();
        vocabulary.findByNormalizedLemmas(command.entries().stream().map(entry -> normalize(entry.lemma())).distinct()
                .toList()).forEach(item -> known.merge(item.getNormalizedLemma(), item,
                ResolveVocabularyUseCase::preferActive));

        Map<String, ResolvedVocabularyResult> resolved = new LinkedHashMap<>();
        for (Entry entry : command.entries()) {
            String key = normalize(entry.lemma());
            if (!resolved.containsKey(key)) {
                resolved.put(key, resolve(entry, key, known.get(key)));
            }
        }
        return List.copyOf(resolved.values());
    }

    private ResolvedVocabularyResult resolve(Entry entry, String key, VocabularyItem existing) {
        if (existing != null && existing.getStatus() != ContentStatus.ACTIVE) {
            throw new InvalidContentStateException("Vocabulary item is inactive: " + entry.lemma().trim());
        }
        Optional<VocabularySense> usable = existing == null ? Optional.empty() : existing.getSenses().stream()
                .filter(sense -> sense.getStatus() == ContentStatus.ACTIVE)
                .min(Comparator.comparingInt(VocabularySense::getSortOrder));
        if (usable.isPresent()) {
            return new ResolvedVocabularyResult(key, usable.get().getId(), false);
        }
        VocabularyItem item = existing != null ? existing
                : vocabulary.save(VocabularyItem.create(entry.lemma(), blankToNull(entry.ipa()), null));
        VocabularySense sense = VocabularySense.create(item.getId(), entry.partOfSpeech(),
                blankToNull(entry.englishDefinition()), entry.vietnameseMeaning().trim(),
                entry.exampleSentence().trim(), null, 0);
        item.addSense(sense);
        vocabulary.save(item);
        return new ResolvedVocabularyResult(key, sense.getId(), true);
    }

    private static VocabularyItem preferActive(VocabularyItem first, VocabularyItem second) {
        return first.getStatus() == ContentStatus.ACTIVE ? first : second;
    }

    static String normalize(String lemma) {
        return lemma.trim().toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

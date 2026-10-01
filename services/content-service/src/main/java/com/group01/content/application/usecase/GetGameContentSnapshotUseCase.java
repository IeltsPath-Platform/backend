package com.group01.content.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group01.content.application.command.GetGameContentSnapshotCommand;
import com.group01.content.application.result.GameContentSnapshotResult;
import com.group01.content.domain.aggregate.Question;
import com.group01.content.domain.aggregate.VocabularyItem;
import com.group01.content.domain.entity.QuestionVersion;
import com.group01.content.domain.entity.VocabularySense;
import com.group01.content.domain.repository.QuestionRepository;
import com.group01.content.domain.repository.VocabularyRepository;
import com.group01.content.domain.vo.ContentStatus;
import com.group01.content.domain.vo.PublicationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetGameContentSnapshotUseCase {
    private final VocabularyRepository vocabularyRepository;
    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper;

    public GetGameContentSnapshotUseCase(VocabularyRepository vocabularyRepository,
                                         QuestionRepository questionRepository, ObjectMapper objectMapper) {
        this.vocabularyRepository = vocabularyRepository;
        this.questionRepository = questionRepository;
        this.objectMapper = objectMapper;
    }

    public GameContentSnapshotResult execute(GetGameContentSnapshotCommand command) {
        if (new HashSet<>(command.contentIds()).size() != command.contentIds().size()) {
            throw new IllegalArgumentException("contentIds must not contain duplicates");
        }
        return switch (command.learningDomain()) {
            case "VOCABULARY" -> vocabularySnapshot(command.gameType(), command.contentIds());
            case "GRAMMAR" -> grammarSnapshot(command.gameType(), command.contentIds());
            default -> throw new IllegalArgumentException("learningDomain must be VOCABULARY or GRAMMAR");
        };
    }

    private GameContentSnapshotResult vocabularySnapshot(String gameType, List<UUID> ids) {
        if (!List.of("WORD_MEANING_MATCH", "SPELLING").contains(gameType)) {
            throw new IllegalArgumentException("gameType is unsupported for vocabulary");
        }
        List<VocabularyItem> items = vocabularyRepository.findByIds(ids);
        if (items.size() != ids.size()) throw new IllegalArgumentException("One or more vocabulary items do not exist");
        List<GameContentSnapshotResult.GameContentItem> snapshots = new ArrayList<>(items.size());
        for (VocabularyItem item : items) {
            if (item.getStatus() != ContentStatus.ACTIVE) continue;
            item.getSenses().stream()
                    .filter(sense -> sense.getStatus() == ContentStatus.ACTIVE)
                    .filter(sense -> sense.getEnglishDefinition() != null && !sense.getEnglishDefinition().isBlank())
                    .min(Comparator.comparingInt(VocabularySense::getSortOrder))
                    .ifPresent(sense -> snapshots.add(new GameContentSnapshotResult.GameContentItem(
                            item.getId(), sense.getId(), null,
                            "SPELLING".equals(gameType) ? sense.getEnglishDefinition()
                                    : item.getLemma() + " — " + sense.getEnglishDefinition(), List.of(),
                            answerSpec("SPELLING".equals(gameType) ? item.getLemma() : sense.getVietnameseMeaning()),
                            sense.getExampleSentence()
                    )));
        }
        return new GameContentSnapshotResult(snapshots);
    }

    private GameContentSnapshotResult grammarSnapshot(String gameType, List<UUID> ids) {
        if (!List.of("SENTENCE_COMPLETION", "ERROR_CORRECTION", "WORD_ORDER").contains(gameType)) {
            throw new IllegalArgumentException("gameType is unsupported for grammar");
        }
        List<Question> questions = questionRepository.findByIds(ids);
        if (questions.size() != ids.size()) throw new IllegalArgumentException("One or more questions do not exist");
        List<GameContentSnapshotResult.GameContentItem> snapshots = new ArrayList<>(questions.size());
        for (Question question : questions) {
            UUID versionId = question.getCurrentPublishedVersionId();
            if (question.getStatus() != PublicationStatus.PUBLISHED || versionId == null) continue;
            question.getVersions().stream()
                    .filter(version -> version.getId().equals(versionId)
                            && version.getStatus() == PublicationStatus.PUBLISHED)
                    .findFirst()
                    .ifPresent(version -> snapshots.add(toSnapshot(question, version)));
        }
        return new GameContentSnapshotResult(snapshots);
    }

    private GameContentSnapshotResult.GameContentItem toSnapshot(Question question, QuestionVersion version) {
        return new GameContentSnapshotResult.GameContentItem(
                question.getId(), null, version.getId(), version.getStem(), version.getOptions(),
                version.getAnswerSpecJson(), version.getExplanation()
        );
    }

    private String answerSpec(String answer) {
        try {
            return objectMapper.writeValueAsString(java.util.Map.of("answer", answer));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to snapshot vocabulary answer", exception);
        }
    }
}

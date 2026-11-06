package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningContentClient.Item;
import com.ieltspath.learning.application.port.LearningContentClient.PackageVersion;
import com.ieltspath.learning.application.port.LearningContentClient.Section;
import com.ieltspath.learning.domain.vo.SpeakingRecording;
import com.ieltspath.learning.domain.vo.SpeakingScope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Which questions a Speaking submission must answer. The parts of a test are its Speaking sections in order, so the part
 * number is the section's place among them (1, 2, 3) and the question number its place in the section. Asking for no
 * parts means the whole test.
 */
@Component
public class SpeakingTestQuestions {
    private final LearningContentClient content;

    public SpeakingTestQuestions(LearningContentClient content) {
        this.content = content;
    }

    public record Resolved(SpeakingScope scope, List<SpeakingRecording> questions) { }

    public Resolved resolve(UUID packageVersionId, List<UUID> partSectionIds) {
        PackageVersion version = content.getPackageVersion(packageVersionId);
        List<Section> parts = version == null || version.sections() == null ? List.of()
                : version.sections().stream()
                .filter(section -> "SPEAKING".equalsIgnoreCase(section.skill()))
                .sorted(Comparator.comparingInt(Section::sortOrder)).toList();
        if (parts.isEmpty()) throw new LearningRequestException(404, "SPEAKING_TEST_NOT_FOUND", "No Speaking test found");

        Set<UUID> wanted = partSectionIds == null || partSectionIds.isEmpty() ? null : new HashSet<>(partSectionIds);
        if (wanted != null) {
            Set<UUID> known = new HashSet<>();
            parts.forEach(part -> known.add(part.sectionId()));
            if (!known.containsAll(wanted)) {
                throw new LearningRequestException(422, "SPEAKING_PART_NOT_FOUND", "A part is not in this test");
            }
        }

        List<SpeakingRecording> questions = new ArrayList<>();
        int included = 0;
        for (int index = 0; index < parts.size(); index++) {
            Section part = parts.get(index);
            if (wanted != null && !wanted.contains(part.sectionId())) continue;
            List<Item> items = part.items() == null ? List.of() : part.items().stream()
                    .sorted(Comparator.comparingInt(Item::sortOrder)).toList();
            if (items.isEmpty()) continue;
            included++;
            for (int question = 0; question < items.size(); question++) {
                questions.add(SpeakingRecording.expected(items.get(question).questionVersionId(), part.sectionId(),
                        index + 1, question + 1, items.get(question).stem()));
            }
        }
        if (questions.isEmpty()) throw new LearningRequestException(422, "SPEAKING_NO_QUESTIONS", "Nothing to record");
        long partsWithQuestions = parts.stream().filter(part -> part.items() != null && !part.items().isEmpty()).count();
        return new Resolved(included == partsWithQuestions ? SpeakingScope.FULL : SpeakingScope.PART, questions);
    }
}

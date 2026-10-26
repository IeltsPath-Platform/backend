package com.ieltspath.content.domain.aggregate;

import com.ieltspath.content.domain.exception.LessonBlockInvalidException;
import com.ieltspath.content.domain.vo.BlockType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LessonBlockTest {
    private final UUID lessonId = UUID.randomUUID();

    @Test void acceptsEachMatchingBlockPayload() {
        assertThat(create(BlockType.TEXT, "Text", null, List.of(), List.of(), List.of())).isNotNull();
        assertThat(create(BlockType.ASSET, null, UUID.randomUUID(), List.of(), List.of(), List.of())).isNotNull();
        assertThat(create(BlockType.VOCABULARY, null, null, List.of(UUID.randomUUID()), List.of(), List.of())).isNotNull();
        assertThat(create(BlockType.EXERCISE, null, null, List.of(), List.of(UUID.randomUUID()), List.of())).isNotNull();
    }

    @Test void rejectsMissingOrIncompatiblePayloads() {
        assertThatThrownBy(() -> create(BlockType.TEXT, null, null, List.of(), List.of(), List.of()))
                .isInstanceOf(LessonBlockInvalidException.class);
        assertThatThrownBy(() -> create(BlockType.ASSET, null, null, List.of(), List.of(), List.of()))
                .isInstanceOf(LessonBlockInvalidException.class);
        assertThatThrownBy(() -> create(BlockType.VOCABULARY, null, null, List.of(), List.of(), List.of()))
                .isInstanceOf(LessonBlockInvalidException.class);
        assertThatThrownBy(() -> create(BlockType.EXERCISE, null, null, List.of(), List.of(), List.of()))
                .isInstanceOf(LessonBlockInvalidException.class);
        assertThatThrownBy(() -> create(BlockType.TEXT, "Text", UUID.randomUUID(), List.of(), List.of(), List.of()))
                .isInstanceOf(LessonBlockInvalidException.class);
    }

    @Test void updatePreservesBlockId() {
        LessonBlock block = LessonBlock.create(lessonId, BlockType.TEXT, 0, "Initial", null, List.of(), List.of(), List.of());
        UUID originalId = block.getId();
        block.update(2, "Updated", null, List.of(), List.of(), List.of());
        assertThat(block.getId()).isEqualTo(originalId);
        assertThat(block.getTextContent()).isEqualTo("Updated");
    }

    private LessonBlock create(BlockType type, String text, UUID asset, List<UUID> senses, List<UUID> questions, List<UUID> points) {
        return LessonBlock.create(lessonId, type, 0, text, asset, senses, questions, points);
    }
}

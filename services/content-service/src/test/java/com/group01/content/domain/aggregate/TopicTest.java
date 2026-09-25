package com.group01.content.domain.aggregate;

import com.group01.content.domain.vo.BandRange;
import java.math.BigDecimal;

import com.group01.content.domain.vo.ContentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TopicTest {

    @Test
    @DisplayName("Should create topic successfully with valid attributes")
    void shouldCreateTopicSuccessfully() {
        Topic topic = Topic.create(null, "IELTS_READING", "IELTS Reading", 1);

        assertThat(topic.getId()).isNotNull();
        assertThat(topic.getCode()).isEqualTo("IELTS_READING");
        assertThat(topic.getName()).isEqualTo("IELTS Reading");
        assertThat(topic.getSortOrder()).isEqualTo(1);
        assertThat(topic.getStatus()).isEqualTo(ContentStatus.ACTIVE);
        assertThat(topic.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should throw exception when creating topic with blank code or name")
    void shouldThrowExceptionWhenCodeOrNameBlank() {
        assertThatThrownBy(() -> Topic.create(null, "", "Name", 1))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> Topic.create(null, "CODE", "  ", 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should update topic successfully")
    void shouldUpdateTopicSuccessfully() {
        Topic topic = Topic.create(null, "IELTS_READING", "Old Name", 1);
        UUID parentId = UUID.randomUUID();

        topic.update(parentId, "New Name", 2, ContentStatus.INACTIVE,
                BandRange.of(new BigDecimal("5.0"), new BigDecimal("6.0")));

        assertThat(topic.getParentTopicId()).isEqualTo(parentId);
        assertThat(topic.getName()).isEqualTo("New Name");
        assertThat(topic.getSortOrder()).isEqualTo(2);
        assertThat(topic.getStatus()).isEqualTo(ContentStatus.INACTIVE);
        assertThat(topic.getBand()).isEqualTo(BandRange.of(new BigDecimal("5.0"), new BigDecimal("6.0")));

        topic.update(parentId, "New Name", 2, null, null);

        assertThat(topic.getBand()).isEqualTo(BandRange.UNBOUNDED);
    }
}


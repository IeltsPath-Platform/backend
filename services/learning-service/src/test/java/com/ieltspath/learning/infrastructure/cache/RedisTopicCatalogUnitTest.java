package com.ieltspath.learning.infrastructure.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.application.port.LearningContentClient;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisTopicCatalogUnitTest {
    private final LearningContentClient content = mock(LearningContentClient.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final SimpleMeterRegistry metrics = new SimpleMeterRegistry();

    @AfterEach
    void closeMetrics() {
        metrics.close();
    }

    @Test
    void disabledCacheReadsTheCorrectContentVariantWithoutTouchingRedis() {
        var base = List.of(new LearningContentClient.Topic(java.util.UUID.randomUUID(), "BASE", "Base", 1,
                List.of()));
        var traps = List.of(new LearningContentClient.Topic(java.util.UUID.randomUUID(), "TRAPS", "Traps", 1,
                List.of()));
        when(content.getTopicSequence()).thenReturn(base);
        when(content.getTopicSequence(true)).thenReturn(traps);
        var catalog = new RedisTopicCatalog(content, redis, new ObjectMapper(), metrics, false, 60);

        assertThat(catalog.topics(false)).isEqualTo(base);
        assertThat(catalog.topics(true)).isEqualTo(traps);
        verify(redis, never()).opsForValue();
    }

    @Test
    void redisReadFailureFallsBackToContentAndCountsAnError() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("cache:learning:topic-sequence:v1:base")).thenThrow(new IllegalStateException("down"));
        var expected = List.of(new LearningContentClient.Topic(java.util.UUID.randomUUID(), "BASE", "Base", 1,
                List.of()));
        when(content.getTopicSequence()).thenReturn(expected);
        var catalog = new RedisTopicCatalog(content, redis, new ObjectMapper(), metrics, true, 60);

        assertThat(catalog.topics(false)).isEqualTo(expected);
        assertThat(metrics.counter("learning.topic.cache", "result", "error").count()).isEqualTo(1);
        verify(content).getTopicSequence();
    }

    @Test
    void redisWriteFailureDoesNotDiscardTheContentResult() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("cache:learning:topic-sequence:v1:base")).thenReturn(null);
        doThrow(new IllegalStateException("down")).when(values).set(org.mockito.ArgumentMatchers.eq("cache:learning:topic-sequence:v1:base"),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(java.time.Duration.class));
        var expected = List.of(new LearningContentClient.Topic(java.util.UUID.randomUUID(), "BASE", "Base", 1,
                List.of()));
        when(content.getTopicSequence()).thenReturn(expected);
        var catalog = new RedisTopicCatalog(content, redis, new ObjectMapper(), metrics, true, 60);

        assertThat(catalog.topics(false)).isEqualTo(expected);
        assertThat(metrics.counter("learning.topic.cache", "result", "miss").count()).isEqualTo(1);
        assertThat(metrics.counter("learning.topic.cache", "result", "error").count()).isEqualTo(1);
    }

    @Test
    void ttlMustBeAtLeastOneSecond() {
        assertThatThrownBy(() -> new RedisTopicCatalog(content, redis, new ObjectMapper(), metrics, false, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("topic cache TTL must be at least one second");
    }
}

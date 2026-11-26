package com.ieltspath.learning.infrastructure.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.domain.vo.LearningSkill;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Testcontainers(disabledWithoutDocker = true)
class RedisTopicCatalogTest {
    private static final TypeReference<List<LearningContentClient.Topic>> TOPIC_LIST = new TypeReference<>() { };
    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    private static LettuceConnectionFactory factory;
    private static StringRedisTemplate redis;
    private static ObjectMapper json;
    private static SimpleMeterRegistry metrics;

    @BeforeAll
    static void connect() {
        var standalone = new RedisStandaloneConfiguration(REDIS.getHost(), REDIS.getMappedPort(6379));
        factory = new LettuceConnectionFactory(standalone, redisClient());
        factory.afterPropertiesSet();
        redis = new StringRedisTemplate(factory);
        redis.afterPropertiesSet();
        json = new ObjectMapper().findAndRegisterModules();
        metrics = new SimpleMeterRegistry();
    }

    @AfterAll
    static void close() {
        if (factory != null) factory.destroy();
        if (metrics != null) metrics.close();
    }

    @BeforeEach
    void clearCacheAndMetrics() {
        redis.delete(List.of("cache:learning:topic-sequence:v1:base",
                "cache:learning:topic-sequence:v1:error-traps"));
        metrics.clear();
    }

    @Test
    void repeatedReadsHitRedisAndSeparateErrorTrapVariants() {
        var content = mock(LearningContentClient.class);
        var base = List.of(topic("base"));
        var traps = List.of(topic("trap"));
        when(content.getTopicSequence()).thenReturn(base);
        when(content.getTopicSequence(true)).thenReturn(traps);
        var catalog = catalog(content, 60);

        assertThat(catalog.topics(false)).isEqualTo(base);
        assertThat(catalog.topics(false)).isEqualTo(base);
        assertThat(catalog.topics(true)).isEqualTo(traps);
        assertThat(catalog.topics(true)).isEqualTo(traps);

        verify(content).getTopicSequence();
        verify(content).getTopicSequence(true);
        assertThat(redis.getExpire("cache:learning:topic-sequence:v1:base", TimeUnit.SECONDS))
                .isBetween(1L, 60L);
        assertThat(redis.getExpire("cache:learning:topic-sequence:v1:error-traps", TimeUnit.SECONDS))
                .isBetween(1L, 60L);
        assertThat(metrics.counter("learning.topic.cache", "result", "miss").count()).isEqualTo(2);
        assertThat(metrics.counter("learning.topic.cache", "result", "hit").count()).isEqualTo(2);
    }

    @Test
    void expiredEntryIsReloadedFromContent() throws InterruptedException {
        redis.delete("cache:learning:topic-sequence:v1:base");
        var content = mock(LearningContentClient.class);
        when(content.getTopicSequence()).thenReturn(List.of(topic("fresh")));
        var catalog = catalog(content, 1);

        catalog.topics(false);
        Thread.sleep(1_150);
        catalog.topics(false);

        verify(content, org.mockito.Mockito.times(2)).getTopicSequence();
    }

    @Test
    void jacksonRoundTripPreservesEntitlementCourseAndKnowledgePointFields() throws Exception {
        redis.delete("cache:learning:topic-sequence:v1:base");
        var content = mock(LearningContentClient.class);
        var expected = List.of(topic("complete"));
        when(content.getTopicSequence()).thenReturn(expected);
        var catalog = catalog(content, 60);

        var first = catalog.topics(false);
        var cachedJson = redis.opsForValue().get("cache:learning:topic-sequence:v1:base");
        var restored = json.readValue(cachedJson, TOPIC_LIST);

        assertThat(first).isEqualTo(expected);
        assertThat(restored).isEqualTo(expected);
        assertThat(restored.getFirst().requiredFeatureKey()).isEqualTo("premium");
        assertThat(restored.getFirst().course().skillUnlockPoints()).containsEntry(LearningSkill.READING, 10);
        var point = restored.getFirst().knowledgePoints().getFirst();
        assertThat(point.parentKnowledgePointId()).isNotNull();
        assertThat(point.errorCode()).isEqualTo("TRAP_1");
        assertThat(point.earliestLessonPosition()).isEqualTo(4);
        verify(content, never()).getTopicSequence(true);
    }

    @Test
    void malformedJsonIsReplacedFromContentAndCountedAsAnError() throws Exception {
        String key = "cache:learning:topic-sequence:v1:base";
        redis.opsForValue().set(key, "not-json", Duration.ofSeconds(60));
        var content = mock(LearningContentClient.class);
        var expected = List.of(topic("replacement"));
        when(content.getTopicSequence()).thenReturn(expected);

        assertThat(catalog(content, 60).topics(false)).isEqualTo(expected);
        assertThat(json.readValue(redis.opsForValue().get(key), TOPIC_LIST)).isEqualTo(expected);
        assertThat(metrics.counter("learning.topic.cache", "result", "error").count()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void contentFailureIsPropagatedAndNullOrEmptyResponsesAreNotStored() {
        String key = "cache:learning:topic-sequence:v1:base";
        redis.delete(key);
        var failure = new IllegalStateException("content unavailable");
        var failingContent = mock(LearningContentClient.class);
        when(failingContent.getTopicSequence()).thenThrow(failure);
        assertThatThrownBy(() -> catalog(failingContent, 60).topics(false)).isSameAs(failure);
        assertThat(redis.hasKey(key)).isFalse();

        var emptyContent = mock(LearningContentClient.class);
        when(emptyContent.getTopicSequence()).thenReturn(List.of());
        catalog(emptyContent, 60).topics(false);
        assertThat(redis.hasKey(key)).isFalse();

        var nullContent = mock(LearningContentClient.class);
        catalog(nullContent, 60).topics(false);
        assertThat(redis.hasKey(key)).isFalse();
    }

    @Test
    void redisOutageFallsBackToContentWithinOneSecond() {
        try (GenericContainer<?> isolatedRedis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379)) {
            isolatedRedis.start();
            var standalone = new RedisStandaloneConfiguration(isolatedRedis.getHost(), isolatedRedis.getMappedPort(6379));
            var outageFactory = new LettuceConnectionFactory(standalone, redisClient());
            outageFactory.afterPropertiesSet();
            var outageTemplate = new StringRedisTemplate(outageFactory);
            outageTemplate.afterPropertiesSet();
            var content = mock(LearningContentClient.class);
            var expected = List.of(topic("fallback"));
            when(content.getTopicSequence()).thenReturn(expected);
            var outageCatalog = new RedisTopicCatalog(content, outageTemplate, json, metrics, true, 60);
            outageCatalog.topics(false);

            isolatedRedis.stop();
            long start = System.nanoTime();
            assertThat(outageCatalog.topics(false)).isEqualTo(expected);
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

            assertThat(elapsedMillis).isLessThan(1_000);
            outageFactory.destroy();
        }
    }

    private static LettuceClientConfiguration redisClient() {
        ClientOptions options = ClientOptions.builder()
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .socketOptions(SocketOptions.builder().connectTimeout(Duration.ofMillis(200)).build())
                .build();
        return LettuceClientConfiguration.builder().commandTimeout(Duration.ofMillis(200)).clientOptions(options).build();
    }

    private RedisTopicCatalog catalog(LearningContentClient content, long ttlSeconds) {
        return new RedisTopicCatalog(content, redis, json, metrics, true, ttlSeconds);
    }

    private static LearningContentClient.Topic topic(String name) {
        UUID pointId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        var point = new LearningContentClient.KnowledgePoint(pointId, "KP_1", name, "LESSON", "READING",
                "Detailed description", true, "B1", "can understand", UUID.randomUUID(), "TRAP_1", 4);
        var course = new LearningContentClient.Course(courseId, "COURSE_6", "Band six", new BigDecimal("6.0"),
                true, true, "cover", "course description", Map.of(LearningSkill.READING, 10), 20,
                List.of(LearningSkill.READING), "banner");
        return new LearningContentClient.Topic(UUID.randomUUID(), "TOPIC_1", "Topic", 3, "premium", List.of(point),
                LearningSkill.READING, true, course, Set.of(LearningSkill.READING), true, 5, "topic description");
    }
}

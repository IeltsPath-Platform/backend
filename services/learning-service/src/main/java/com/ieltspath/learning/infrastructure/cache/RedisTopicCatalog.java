package com.ieltspath.learning.infrastructure.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.TopicCatalog;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public class RedisTopicCatalog implements TopicCatalog {
    private static final Logger log = LoggerFactory.getLogger(RedisTopicCatalog.class);
    private static final String BASE_KEY = "cache:learning:topic-sequence:v1:base";
    private static final String ERROR_TRAPS_KEY = "cache:learning:topic-sequence:v1:error-traps";
    private static final TypeReference<List<LearningContentClient.Topic>> TOPIC_LIST = new TypeReference<>() { };

    private final LearningContentClient content;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final MeterRegistry metrics;
    private final boolean enabled;
    private final Duration ttl;

    public RedisTopicCatalog(LearningContentClient content, StringRedisTemplate redis, ObjectMapper objectMapper,
                             MeterRegistry metrics,
                             @Value("${learning.topic-cache.enabled:false}") boolean enabled,
                             @Value("${learning.topic-cache.ttl-seconds:60}") long ttlSeconds) {
        if (ttlSeconds < 1) throw new IllegalArgumentException("topic cache TTL must be at least one second");
        this.content = content;
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.metrics = metrics;
        this.enabled = enabled;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    @Override
    public List<LearningContentClient.Topic> topics(boolean includeErrorTraps) {
        if (!enabled) return TopicCatalog.direct(content).topics(includeErrorTraps);

        String key = includeErrorTraps ? ERROR_TRAPS_KEY : BASE_KEY;
        List<LearningContentClient.Topic> cached = read(key);
        if (cached != null) return cached;

        List<LearningContentClient.Topic> topics = includeErrorTraps
                ? content.getTopicSequence(true) : content.getTopicSequence();
        if (topics != null && !topics.isEmpty()) write(key, topics);
        return topics;
    }

    private List<LearningContentClient.Topic> read(String key) {
        try {
            String value = redis.opsForValue().get(key);
            if (value == null) {
                count("miss");
                return null;
            }
            List<LearningContentClient.Topic> topics = objectMapper.readValue(value, TOPIC_LIST);
            if (topics == null || topics.isEmpty()) {
                count("error");
                log.warn("Topic catalog cache contained an empty value for key {}", key);
                return null;
            }
            count("hit");
            return topics;
        } catch (Exception failure) {
            count("error");
            log.warn("Topic catalog cache read failed for key {} ({})", key, failure.getClass().getSimpleName());
            return null;
        }
    }

    private void write(String key, List<LearningContentClient.Topic> topics) {
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(topics), ttl);
        } catch (Exception failure) {
            count("error");
            log.warn("Topic catalog cache write failed for key {} ({})", key, failure.getClass().getSimpleName());
        }
    }

    private void count(String result) {
        metrics.counter("learning.topic.cache", "result", result).increment();
    }
}

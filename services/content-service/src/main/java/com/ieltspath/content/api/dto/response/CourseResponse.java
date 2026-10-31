package com.ieltspath.content.api.dto.response;

import com.ieltspath.content.application.result.CourseResult;
import com.ieltspath.content.domain.vo.ContentStatus;
import com.ieltspath.content.domain.vo.AssetType;
import com.ieltspath.content.domain.vo.MediaReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.Map;
import com.ieltspath.content.domain.vo.Skill;

public record CourseResponse(UUID id, String code, String name, BigDecimal bandLevel, ContentStatus status,
                             Instant createdAt, Instant updatedAt, String coverImageUrl, String description,
                             Map<Skill, Integer> skillUnlockPoints, Integer comboUnlockPoints) {
    private static final Logger log = LoggerFactory.getLogger(CourseResponse.class);
    public CourseResponse(UUID id, String code, String name, BigDecimal bandLevel, ContentStatus status,
                          Instant createdAt, Instant updatedAt) {
        this(id, code, name, bandLevel, status, createdAt, updatedAt, null, null, Map.of(), null);
    }
    public CourseResponse(UUID id, String code, String name, BigDecimal bandLevel, ContentStatus status,
                          Instant createdAt, Instant updatedAt, String coverImageUrl, String description) {
        this(id, code, name, bandLevel, status, createdAt, updatedAt, coverImageUrl, description, Map.of(), null);
    }

    public static CourseResponse from(CourseResult result) {
        return from(result, null);
    }

    public static CourseResponse from(CourseResult result, MediaReferencePolicy mediaPolicy) {
        return new CourseResponse(result.id(), result.code(), result.name(), result.bandLevel(), result.status(),
                result.createdAt(), result.updatedAt(), resolveCoverImage(result.coverImageUrl(), mediaPolicy), result.description(),
                result.skillUnlockPoints(), result.comboUnlockPoints());
    }

    private static String resolveCoverImage(String reference, MediaReferencePolicy mediaPolicy) {
        if (reference == null || !reference.startsWith("content-image/") || mediaPolicy == null) return reference;
        try {
            return mediaPolicy.resolve(AssetType.IMAGE, reference);
        } catch (RuntimeException unresolvable) {
            log.warn("Course cover image key could not be resolved");
            return null;
        }
    }
}

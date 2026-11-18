package com.ieltspath.assessment.api.dto.response;

import com.ieltspath.assessment.application.result.PlacementResult;
import com.ieltspath.assessment.domain.vo.PlacementPool;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PlacementResultResponseTest {
    @Test
    void mapsPoolCeilingPartialAndLegacyDefaults() {
        UUID attempt = UUID.randomUUID();
        var capped = PlacementResultResponse.from(new PlacementResult(attempt, 3.0, Instant.now(), List.of(), List.of(),
                PlacementPool.BAND_0_3, 3.0, true, false));
        assertThat(capped.pool()).isEqualTo(PlacementPool.BAND_0_3);
        assertThat(capped.ceiling()).isEqualTo(3.0);
        assertThat(capped.reachedCeiling()).isTrue();
        assertThat(capped.partial()).isFalse();

        var legacy = PlacementResultResponse.from(new PlacementResult(attempt, 6.0, Instant.now(), List.of()));
        assertThat(legacy.pool()).isNull();
        assertThat(legacy.ceiling()).isNull();
        assertThat(legacy.reachedCeiling()).isFalse();
        assertThat(legacy.partial()).isFalse();
    }
}

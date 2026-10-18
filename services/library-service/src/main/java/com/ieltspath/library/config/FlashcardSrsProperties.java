package com.ieltspath.library.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "library.srs")
public class FlashcardSrsProperties {
    private List<Integer> intervalsDays = List.of(1, 2, 4, 8, 16);

    public List<Integer> getIntervalsDays() {
        return intervalsDays;
    }

    public void setIntervalsDays(List<Integer> intervalsDays) {
        if (intervalsDays == null || intervalsDays.size() != 5
                || intervalsDays.stream().anyMatch(days -> days == null || days < 1)) {
            throw new IllegalArgumentException("library.srs.intervals-days must contain five positive day values");
        }
        this.intervalsDays = List.copyOf(intervalsDays);
    }
}

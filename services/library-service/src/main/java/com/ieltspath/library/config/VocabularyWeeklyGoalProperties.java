package com.ieltspath.library.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "library.vocabulary")
public class VocabularyWeeklyGoalProperties {
    private int weeklyGoalDefault = 120;

    public int getWeeklyGoalDefault() { return weeklyGoalDefault; }
    public void setWeeklyGoalDefault(int weeklyGoalDefault) {
        if (weeklyGoalDefault < 10 || weeklyGoalDefault > 1000) {
            throw new IllegalArgumentException("library.vocabulary.weekly-goal-default must be between 10 and 1000");
        }
        this.weeklyGoalDefault = weeklyGoalDefault;
    }
}

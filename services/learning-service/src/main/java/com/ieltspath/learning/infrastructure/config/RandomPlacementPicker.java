package com.ieltspath.learning.infrastructure.config;

import com.ieltspath.learning.application.port.PlacementPicker;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class RandomPlacementPicker implements PlacementPicker {
    @Override public int choose(int bound) { return ThreadLocalRandom.current().nextInt(bound); }
}

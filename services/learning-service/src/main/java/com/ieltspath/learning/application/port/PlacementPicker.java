package com.ieltspath.learning.application.port;

@FunctionalInterface
public interface PlacementPicker {
    int choose(int bound);
}

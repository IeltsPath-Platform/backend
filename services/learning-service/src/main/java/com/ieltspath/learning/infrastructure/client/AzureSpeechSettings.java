package com.ieltspath.learning.infrastructure.client;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * {@code learning.speech.*}. A blank {@code key} or {@code region} leaves the speech analysis off (the service still
 * runs; analysing a recording is then refused). {@code locale} decides the accent the pronunciation is judged against
 * ({@code en-US} also gives an intonation score). {@code maxConcurrent} caps the analyses running at once.
 */
@Validated
@ConfigurationProperties("learning.speech")
public record AzureSpeechSettings(
        @DefaultValue("") String key,
        @DefaultValue("") String region,
        @DefaultValue("en-US") @NotBlank String locale,
        @DefaultValue("180") @Min(10) int timeoutSeconds,
        @DefaultValue("2") @Min(1) int maxConcurrent) {

    public boolean configured() {
        return !key.isBlank() && !region.isBlank();
    }
}

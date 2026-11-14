package com.ieltspath.learning.application.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** The Writing settings bind from the configuration keys, including the density rules. */
class WritingSettingsBindingTest {
    @Test
    void theDensityKeysBindToTheirRules() {
        var source = new MapConfigurationPropertySource(Map.of(
                "learning.writing.density.grammar-share", "0.8",
                "learning.writing.density.grammar-band-cap", "4.0",
                "learning.writing.density.wrong-data-overview-band-cap", "3.0"));

        WritingSettings settings = new Binder(source).bind("learning.writing", WritingSettings.class).get();

        var rules = settings.density().rules();
        assertEquals(0, new BigDecimal("0.8").compareTo(rules.grammarShare()));
        assertEquals(0, new BigDecimal("4.0").compareTo(rules.grammarCap()));
        assertEquals(0, new BigDecimal("3.0").compareTo(rules.wrongDataOverviewCap()));
        assertNull(rules.vocabularyCap(), "a cap that is not set stays off");
    }

    @Test
    void aBlankCapBindsAsOff() {
        var source = new MapConfigurationPropertySource(Map.of(
                "learning.writing.density.grammar-band-cap", "",
                "learning.writing.density.vocabulary-band-cap", "4.0"));

        var rules = new Binder(source).bind("learning.writing", WritingSettings.class).get().density().rules();

        assertNull(rules.grammarCap());
        assertEquals(0, new BigDecimal("4.0").compareTo(rules.vocabularyCap()));
    }

    @Test
    void withoutDensityKeysEveryDensityRuleIsOff() {
        WritingSettings settings = new Binder(new MapConfigurationPropertySource(Map.of("learning.writing.point-cost", "3")))
                .bind("learning.writing", WritingSettings.class).get();

        var rules = settings.density().rules();
        assertNull(rules.grammarCap());
        assertNull(rules.vocabularyCap());
        assertNull(rules.wrongDataCap());
        assertNull(rules.wrongDataOverviewCap());
    }
}

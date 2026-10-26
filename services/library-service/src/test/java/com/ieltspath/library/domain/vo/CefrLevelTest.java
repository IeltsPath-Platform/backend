package com.ieltspath.library.domain.vo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CefrLevelTest {
    @Test void mapsEveryCefrLevelToItsIeltsBandRange() {
        assertEquals(new IeltsBandRange(1.0,2.5),CefrLevel.A1.bandRange());
        assertEquals(new IeltsBandRange(3.0,3.5),CefrLevel.A2.bandRange());
        assertEquals(new IeltsBandRange(4.0,5.0),CefrLevel.B1.bandRange());
        assertEquals(new IeltsBandRange(5.5,6.5),CefrLevel.B2.bandRange());
        assertEquals(new IeltsBandRange(7.0,8.0),CefrLevel.C1.bandRange());
        assertEquals(new IeltsBandRange(8.5,9.0),CefrLevel.C2.bandRange());
    }
}

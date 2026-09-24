package com.group01.learningsupport.api.dto.response;

import com.group01.learningsupport.application.result.PageResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageResponseTest {
    @Test
    void mapsItemsAndPreservesExistingPageMetadata() {
        var response = PageResponse.from(new PageResult<>(List.of("first", "second"), 2, 10, 25), String::toUpperCase);

        assertEquals(List.of("FIRST", "SECOND"), response.items());
        assertEquals(2, response.page());
        assertEquals(10, response.size());
        assertEquals(25, response.total());
    }
}

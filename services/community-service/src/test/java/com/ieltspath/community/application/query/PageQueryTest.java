package com.ieltspath.community.application.query;

import com.ieltspath.community.domain.exception.CommunityException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PageQueryTest {

    @Test
    void sizeZeroIsRejected() {
        var error = assertThrows(CommunityException.class, () -> new PageQuery(0, 0));
        assertEquals("Size must be between 1 and 100", error.getMessage());
    }

    @Test
    void sizeOneAndSizeOneHundredAreAccepted() {
        assertEquals(1, new PageQuery(0, 1).size());
        assertEquals(100, new PageQuery(0, 100).size());
    }

    @Test
    void sizeOneHundredAndOneIsRejected() {
        var error = assertThrows(CommunityException.class, () -> new PageQuery(0, 101));
        assertEquals("Size must be between 1 and 100", error.getMessage());
    }

    @Test
    void negativePageIsRejected() {
        var error = assertThrows(CommunityException.class, () -> new PageQuery(-1, 10));
        assertEquals("Page must be non-negative", error.getMessage());
    }
}

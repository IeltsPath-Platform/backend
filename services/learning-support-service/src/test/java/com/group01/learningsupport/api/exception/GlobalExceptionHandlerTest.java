package com.group01.learningsupport.api.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new StaleWriteController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void staleWriteReturnsConflictInsteadOfInternalServerError() throws Exception {
        mvc.perform(put("/api/learning-support/flashcards/123").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Dữ liệu vừa được thay đổi; vui lòng tải lại và thử lại"))
                .andExpect(jsonPath("$.path").value("/api/learning-support/flashcards/123"));
    }

    @RestController
    static class StaleWriteController {
        @PutMapping("/api/learning-support/flashcards/{id}")
        void fail(@PathVariable("id") String id) {
            throw new OptimisticLockingFailureException("The flashcard changed concurrently");
        }
    }
}

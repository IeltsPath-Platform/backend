package com.ieltspath.content.api.controller;

import com.ieltspath.content.api.exception.GlobalExceptionHandler;
import com.ieltspath.content.application.result.ProgressMetadataResult;
import com.ieltspath.content.application.usecase.GetProgressMetadataUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import java.util.List;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class InternalProgressMetadataControllerTest {
    @Test void validatesBatchBeforeCallingReaderAndReturnsOnlyMetadata() throws Exception {
        var useCase=mock(GetProgressMetadataUseCase.class);
        var validator=new LocalValidatorFactoryBean(); validator.afterPropertiesSet();
        var mvc=MockMvcBuilders.standaloneSetup(new InternalProgressMetadataController(useCase))
                .setValidator(validator).setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(post("/internal/learning-content/progress-metadata").contentType(MediaType.APPLICATION_JSON)
                .content("{\"courseIds\":[]}")).andExpect(status().isBadRequest());
        mvc.perform(post("/internal/learning-content/progress-metadata").contentType(MediaType.APPLICATION_JSON)
                .content("{\"courseIds\":[null]}")).andExpect(status().isBadRequest());
        verifyNoInteractions(useCase);
        UUID course=UUID.randomUUID();
        when(useCase.execute(List.of(course))).thenReturn(new ProgressMetadataResult("revision",true,List.of()));
        mvc.perform(post("/internal/learning-content/progress-metadata").contentType(MediaType.APPLICATION_JSON)
                .content("{\"courseIds\":[\""+course+"\"]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.revision").value("revision"))
                .andExpect(jsonPath("$.complete").value(true));
        validator.close();
    }
}

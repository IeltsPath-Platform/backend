package com.group01.content.api.controller;

import com.group01.content.api.exception.GlobalExceptionHandler;
import com.group01.content.application.usecase.*;
import com.group01.content.domain.aggregate.Course;
import com.group01.content.domain.repository.CourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CourseControllerTest {
    private final CourseRepository repository = mock(CourseRepository.class);
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new CourseController(new CreateCourseUseCase(repository),
                        new UpdateCourseUseCase(repository), new ListCoursesUseCase(repository)))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void createsACourseAndReturnsItsBand() throws Exception {
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        mvc.perform(post("/api/content/admin/courses").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"IELTS_5_5\",\"name\":\"IELTS 5.5\",\"bandLevel\":5.5}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.code").value("IELTS_5_5"))
                .andExpect(jsonPath("$.bandLevel").value(5.5)).andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void duplicateBandOrCodeReturnsConflictWithoutSaving() throws Exception {
        when(repository.findByBandLevel(new BigDecimal("5.5")))
                .thenReturn(Optional.of(Course.create("OLD", "Existing", new BigDecimal("5.5"))));
        mvc.perform(post("/api/content/admin/courses").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"NEW\",\"name\":\"New\",\"bandLevel\":5.5}"))
                .andExpect(status().isConflict());
        when(repository.existsByCode("OLD")).thenReturn(true);
        mvc.perform(post("/api/content/admin/courses").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"OLD\",\"name\":\"New\",\"bandLevel\":6.5}"))
                .andExpect(status().isConflict());
        verify(repository, never()).save(any());
    }

    @Test
    void invalidBandOrMissingNameReturnsBadRequest() throws Exception {
        for (String band : new String[]{"5.25", "9.5", "null"}) {
            mvc.perform(post("/api/content/admin/courses").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"code\":\"IELTS\",\"name\":\"IELTS\",\"bandLevel\":" + band + "}"))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/content/admin/courses").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"IELTS\",\"name\":\" \",\"bandLevel\":5.5}"))
                .andExpect(status().isBadRequest());
        verify(repository, never()).save(any());
    }

    @Test
    void listsCoursesInRepositoryBandOrder() throws Exception {
        when(repository.findAllOrderByBandLevel()).thenReturn(List.of(
                Course.create("LOW", "Low", new BigDecimal("5.5")),
                Course.create("HIGH", "High", new BigDecimal("6.5"))));
        mvc.perform(get("/api/content/courses")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bandLevel").value(5.5))
                .andExpect(jsonPath("$[1].bandLevel").value(6.5));
        verify(repository).findAllOrderByBandLevel();
    }

    @Test
    void updatesNameBandAndStatusAndAllowsTheCurrentBand() throws Exception {
        Course course = Course.create("IELTS", "Old", new BigDecimal("5.5"));
        when(repository.findById(course.getId())).thenReturn(Optional.of(course));
        when(repository.findByBandLevel(new BigDecimal("5.5"))).thenReturn(Optional.of(course));
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        mvc.perform(put("/api/content/admin/courses/{id}", course.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated\",\"bandLevel\":5.5,\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Updated"))
                .andExpect(jsonPath("$.code").value("IELTS"))
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void updateRejectsAnotherCoursesBandAndUnknownId() throws Exception {
        Course course = Course.create("IELTS", "Old", new BigDecimal("5.5"));
        when(repository.findById(course.getId())).thenReturn(Optional.of(course));
        when(repository.findByBandLevel(new BigDecimal("6.5")))
                .thenReturn(Optional.of(Course.create("OTHER", "Other", new BigDecimal("6.5"))));
        mvc.perform(put("/api/content/admin/courses/{id}", course.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated\",\"bandLevel\":6.5}"))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/content/admin/courses/{id}", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated\",\"bandLevel\":5.5}"))
                .andExpect(status().isNotFound());
        verify(repository, never()).save(any());
    }
}

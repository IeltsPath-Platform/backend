package com.ieltspath.content.api.controller;

import com.ieltspath.content.api.exception.GlobalExceptionHandler;
import com.ieltspath.content.application.usecase.*;
import com.ieltspath.content.domain.aggregate.Course;
import com.ieltspath.content.domain.repository.CourseRepository;
import com.ieltspath.content.domain.vo.MediaReferencePolicy;
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
                        new UpdateCourseUseCase(repository), new ListCoursesUseCase(repository),
                        new MediaReferencePolicy("https://media.example.test/storage/v1/object/public/media")))
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
    void createsAndUpdatesCourseCoverAndDescription() throws Exception {
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        String cover = "https://images.example/course.jpg";
        mvc.perform(post("/api/content/admin/courses").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"IELTS_5_5\",\"name\":\"IELTS 5.5\",\"bandLevel\":5.5,\"coverImageUrl\":\" " + cover + " \",\"description\":\"  Course intro  \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.coverImageUrl").value(cover))
                .andExpect(jsonPath("$.description").value("Course intro"));

        Course course = Course.create("UPDATE", "Old", new BigDecimal("6.5"));
        when(repository.findById(course.getId())).thenReturn(Optional.of(course));
        when(repository.findByBandLevel(new BigDecimal("6.5"))).thenReturn(Optional.of(course));
        mvc.perform(put("/api/content/admin/courses/{id}", course.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated\",\"bandLevel\":6.5,\"coverImageUrl\":\"https://images.example/new.jpg\",\"description\":\"Updated intro\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverImageUrl").value("https://images.example/new.jpg"))
                .andExpect(jsonPath("$.description").value("Updated intro"));
        mvc.perform(put("/api/content/admin/courses/{id}", course.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated\",\"bandLevel\":6.5,\"coverImageUrl\":\" \",\"description\":\" \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverImageUrl").doesNotExist())
                .andExpect(jsonPath("$.description").doesNotExist());
    }

    @Test
    void acceptsCourseSkillPricesAndCombo() throws Exception {
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        mvc.perform(post("/api/content/admin/courses").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"PRICED","name":"Priced","bandLevel":5.5,
                                 "skillUnlockPoints":{"READING":120,"WRITING":150},"comboUnlockPoints":250}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.skillUnlockPoints.READING").value(120))
                .andExpect(jsonPath("$.skillUnlockPoints.WRITING").value(150))
                .andExpect(jsonPath("$.skillUnlockPoints.LISTENING").doesNotExist())
                .andExpect(jsonPath("$.comboUnlockPoints").value(250));

        Course course = Course.create("UPDATE", "Old", new BigDecimal("6.5"));
        when(repository.findById(course.getId())).thenReturn(Optional.of(course));
        when(repository.findByBandLevel(new BigDecimal("6.5"))).thenReturn(Optional.of(course));
        mvc.perform(put("/api/content/admin/courses/{id}", course.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Updated","bandLevel":6.5,
                                 "skillUnlockPoints":{"LISTENING":80},"comboUnlockPoints":80}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skillUnlockPoints.LISTENING").value(80))
                .andExpect(jsonPath("$.skillUnlockPoints.READING").doesNotExist())
                .andExpect(jsonPath("$.comboUnlockPoints").value(80));
    }

    @Test
    void rejectsInvalidCourseSkillPricesAndCombos() throws Exception {
        for (String body : new String[]{
                "{\"code\":\"BAD\",\"name\":\"Bad\",\"bandLevel\":5.5,\"skillUnlockPoints\":{\"READING\":0}}",
                "{\"code\":\"BAD\",\"name\":\"Bad\",\"bandLevel\":5.5,\"skillUnlockPoints\":{\"READING\":-1}}",
                "{\"code\":\"BAD\",\"name\":\"Bad\",\"bandLevel\":5.5,\"skillUnlockPoints\":{\"ALL\":100}}",
                "{\"code\":\"BAD\",\"name\":\"Bad\",\"bandLevel\":5.5,\"skillUnlockPoints\":{\"READING\":100},\"comboUnlockPoints\":99}",
                "{\"code\":\"BAD\",\"name\":\"Bad\",\"bandLevel\":5.5,\"skillUnlockPoints\":{\"READING\":100,\"WRITING\":100},\"comboUnlockPoints\":201}"
        }) {
            mvc.perform(post("/api/content/admin/courses").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsUnsafeCourseCoverImageSchemes() throws Exception {
        for (String url : new String[]{"javascript:alert(1)", "data:image/png;base64,AAAA"}) {
            mvc.perform(post("/api/content/admin/courses").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"code\":\"IELTS\",\"name\":\"IELTS\",\"bandLevel\":5.5,\"coverImageUrl\":\"" + url + "\"}"))
                    .andExpect(status().isBadRequest());
        }
        verify(repository, never()).save(any());
    }

    @Test
    void acceptsSafeImageKeysAndRejectsInvalidKeys() throws Exception {
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        mvc.perform(post("/api/content/admin/courses").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"KEYED\",\"name\":\"Keyed\",\"bandLevel\":5.5,\"coverImageUrl\":\"content-image/user-1/cover.png\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.coverImageUrl").value("https://media.example.test/storage/v1/object/public/media/content-image/user-1/cover.png"));

        for (String key : new String[]{"content-image/../cover.png", "/content-image/user/cover.png", "media/user/cover.png"}) {
            mvc.perform(post("/api/content/admin/courses").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"code\":\"INVALID\",\"name\":\"Invalid\",\"bandLevel\":5.5,\"coverImageUrl\":\"" + key + "\"}"))
                    .andExpect(status().isBadRequest());
        }
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

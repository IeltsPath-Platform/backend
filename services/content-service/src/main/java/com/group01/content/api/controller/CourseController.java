package com.group01.content.api.controller;

import com.group01.content.api.dto.request.CreateCourseRequest;
import com.group01.content.api.dto.request.UpdateCourseRequest;
import com.group01.content.api.dto.response.CourseResponse;
import com.group01.content.application.command.CreateCourseCommand;
import com.group01.content.application.command.UpdateCourseCommand;
import com.group01.content.application.usecase.CreateCourseUseCase;
import com.group01.content.application.usecase.UpdateCourseUseCase;
import com.group01.content.application.usecase.ListCoursesUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class CourseController {
    private final CreateCourseUseCase createCourse;
    private final UpdateCourseUseCase updateCourse;
    private final ListCoursesUseCase listCourses;

    @GetMapping("/api/content/courses")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_AUTHOR', 'CUSTOMER', 'EXAMINER')")
    public List<CourseResponse> list() {
        return listCourses.execute().stream().map(CourseResponse::from).toList();
    }

    @PostMapping("/api/content/admin/courses")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_AUTHOR')")
    public CourseResponse create(@Valid @RequestBody CreateCourseRequest request) {
        return CourseResponse.from(createCourse.execute(new CreateCourseCommand(request.code(), request.name(), request.bandLevel())));
    }

    @PutMapping("/api/content/admin/courses/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_AUTHOR')")
    public CourseResponse update(@PathVariable("id") UUID id, @Valid @RequestBody UpdateCourseRequest request) {
        return CourseResponse.from(updateCourse.execute(new UpdateCourseCommand(id, request.name(), request.bandLevel(), request.status())));
    }
}

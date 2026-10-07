package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.api.dto.response.CourseResponse;
import com.ieltspath.learning.api.dto.response.TestAssignmentResponse;
import com.ieltspath.learning.application.usecase.AssignCourseTestUseCase;
import com.ieltspath.learning.application.usecase.ListCoursesUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/learning/courses")
@RequiredArgsConstructor
public class CourseController {
    private final CurrentUserProvider currentUser;
    private final ListCoursesUseCase listCourses;
    private final AssignCourseTestUseCase assignCourseTest;

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public List<CourseResponse> courses() {
        return listCourses.execute(currentUser.requireUserId()).stream().map(CourseResponse::from).toList();
    }

    @PostMapping("/{id}/test-assignments")
    @PreAuthorize("hasRole('CUSTOMER')")
    public TestAssignmentResponse assign(@PathVariable UUID id) {
        return TestAssignmentResponse.from(assignCourseTest.execute(currentUser.requireUserId(), id));
    }
}

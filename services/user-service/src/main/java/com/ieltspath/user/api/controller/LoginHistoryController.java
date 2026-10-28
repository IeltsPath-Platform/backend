package com.ieltspath.user.api.controller;

import com.ieltspath.user.api.dto.response.LoginHistoryResponse;
import com.ieltspath.user.api.dto.response.PageResponse;
import com.ieltspath.user.application.query.PageQuery;
import com.ieltspath.user.application.usecase.GetLoginHistoryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class LoginHistoryController {
    private final GetLoginHistoryUseCase getLoginHistoryUseCase;

    @GetMapping("/{id}/login-history")
    public PageResponse<LoginHistoryResponse> getLoginHistory(
            @PathVariable("id") UUID id,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        return PageResponse.from(getLoginHistoryUseCase.execute(id, new PageQuery(page, size)), LoginHistoryResponse::from);
    }
}

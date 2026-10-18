package com.ieltspath.community.api.dto.request;

import com.ieltspath.community.domain.vo.ReportContentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateContentReportRequest(@NotNull ReportContentType contentType, @NotNull UUID contentId,
                                         @NotBlank @Size(max = 1000) String reason) { }

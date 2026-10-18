package com.ieltspath.community.api.dto.request;

import com.ieltspath.community.domain.vo.ReportDecision;
import jakarta.validation.constraints.NotNull;

public record ResolveContentReportRequest(@NotNull ReportDecision decision) { }

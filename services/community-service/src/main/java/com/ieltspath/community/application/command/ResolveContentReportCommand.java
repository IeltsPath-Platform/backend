package com.ieltspath.community.application.command;

import com.ieltspath.community.domain.vo.ReportDecision;

import java.util.UUID;

public record ResolveContentReportCommand(UUID reportId, ReportDecision decision) { }

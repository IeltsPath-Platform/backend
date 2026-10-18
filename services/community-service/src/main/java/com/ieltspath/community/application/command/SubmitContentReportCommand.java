package com.ieltspath.community.application.command;

import com.ieltspath.community.domain.vo.ReportContentType;

import java.util.UUID;

public record SubmitContentReportCommand(ReportContentType contentType, UUID contentId, String reason) { }

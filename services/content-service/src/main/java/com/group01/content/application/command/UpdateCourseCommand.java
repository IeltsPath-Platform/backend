package com.group01.content.application.command;

import com.group01.content.domain.vo.ContentStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record UpdateCourseCommand(UUID id, String name, BigDecimal bandLevel, ContentStatus status) {}

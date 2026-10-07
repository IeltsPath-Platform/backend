package com.group01.content.application.command;

import java.math.BigDecimal;

public record CreateCourseCommand(String code, String name, BigDecimal bandLevel) {}

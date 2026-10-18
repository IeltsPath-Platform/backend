package com.ieltspath.user.application.result;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InternalLearningGoalResult(BigDecimal targetBand, LocalDate examDate) {}

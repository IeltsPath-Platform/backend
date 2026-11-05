package com.ieltspath.user.application.command;

import com.ieltspath.user.application.result.ExaminerResult;

import java.math.BigDecimal;
import java.util.List;

/** What an examiner keeps about themselves: the band, the certificates and whether they take new submissions. */
public record UpdateExaminerProfileCommand(BigDecimal ieltsBand, List<ExaminerResult.Certificate> certificates,
                                           boolean acceptingSubmissions) {}

package com.ieltspath.library.application.query;

import com.ieltspath.library.domain.vo.UnderstandingLevel;

import java.util.List;

public record NotebookQuery(String q, List<UnderstandingLevel> levels, Boolean due, String sort, int page, int size) {}

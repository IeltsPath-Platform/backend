package com.ieltspath.learning.application.result;

import java.util.List;

public record WorkItemPage(List<WorkItemView> content, int page, int size, long totalElements, int totalPages) {
    public WorkItemPage { content = List.copyOf(content); }
}

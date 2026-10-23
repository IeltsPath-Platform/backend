package com.ieltspath.learning.application.result;

import com.ieltspath.learning.domain.vo.StaffRecords.WorkItem;

import java.util.Map;

/** A work item and what it is about; {@code subject} is null for Examiner items or when it cannot be named. */
public record WorkItemView(WorkItem item, Map<String, Object> subject) {
}

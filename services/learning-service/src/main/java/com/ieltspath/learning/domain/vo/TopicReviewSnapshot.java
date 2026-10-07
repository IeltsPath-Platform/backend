package com.ieltspath.learning.domain.vo;

import java.util.List;

public record TopicReviewSnapshot(List<PendingReview> pending, List<PracticeReviewState> practice) {}

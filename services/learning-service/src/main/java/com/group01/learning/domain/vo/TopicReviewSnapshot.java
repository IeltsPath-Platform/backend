package com.group01.learning.domain.vo;

import java.util.List;

public record TopicReviewSnapshot(List<PendingReview> pending, List<PracticeReviewState> practice) {}

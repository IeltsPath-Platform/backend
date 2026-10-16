package com.ieltspath.content.api.dto.response;
import java.util.List;
public record PracticeTestPageResponse(long totalElements, int page, int size, List<PracticeTestCardResponse> items) {}

package com.ieltspath.content.api.dto.request;

import com.ieltspath.content.domain.vo.QuestionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public final class SectionQuestionDraftRequests {
    private SectionQuestionDraftRequests() {}
    public record Request(@NotNull(message = "Cần chọn gói đề.") UUID packageId,
                          @NotNull(message = "Cần chọn phiên bản đề.") UUID versionId,
                          @NotNull(message = "Cần chọn phần.") UUID sectionId,
                          @NotNull(message = "Cần chọn dạng câu hỏi.") QuestionType questionType,
                          @Min(value = 1, message = "Số câu phải từ 1 đến 10.") @Max(value = 10, message = "Số câu phải từ 1 đến 10.") int count,
                          @Size(max = 500, message = "Hướng dẫn thêm không được dài quá 500 ký tự") String instructions) {}
}

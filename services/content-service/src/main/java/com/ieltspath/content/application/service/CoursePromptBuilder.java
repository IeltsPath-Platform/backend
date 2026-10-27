package com.ieltspath.content.application.service;

import com.ieltspath.content.application.command.CourseDraftRequest;
import com.ieltspath.content.application.result.CourseCandidates;
import com.ieltspath.content.application.result.CourseCandidates.Candidate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Builds the course prompt from the candidate set and the request only: knowledge point codes, names and short
 * descriptions, practice set codes and short question labels. No question text and no learner data is included, and the
 * model is told it may only name what is listed.
 */
final class CoursePromptBuilder {

    static final String VERSION = "COURSE-v2";
    private static final int DESCRIPTION_LIMIT = 200;

    private CoursePromptBuilder() {
    }

    /** Short labels for the candidate questions, in order. A question shared by two points keeps its first label. */
    static Map<String, UUID> questionLabels(CourseCandidates candidates) {
        Map<String, UUID> labels = new LinkedHashMap<>();
        Map<UUID, String> seen = new LinkedHashMap<>();
        for (Candidate point : candidates.points()) {
            for (UUID question : point.questions()) {
                if (!seen.containsKey(question)) {
                    String label = "Q" + (seen.size() + 1);
                    seen.put(question, label);
                    labels.put(label, question);
                }
            }
        }
        return labels;
    }

    static String build(CourseDraftRequest request, CourseCandidates candidates) {
        Map<UUID, String> labelOf = questionLabels(candidates).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey, (a, b) -> a, LinkedHashMap::new));
        StringBuilder points = new StringBuilder();
        for (Candidate point : candidates.points()) {
            points.append("- ").append(point.code()).append(" | ").append(single(point.name())).append(" | ")
                    .append(shorten(point.description())).append(point.foundation() ? " | NỀN" : "").append('\n');
            if (!point.practiceSets().isEmpty()) {
                points.append("    gói luyện: ").append(point.practiceSets().stream()
                        .map(set -> set.code()).collect(Collectors.joining(", "))).append('\n');
            }
            if (!point.questions().isEmpty()) {
                points.append("    câu hỏi: ").append(point.questions().stream().map(labelOf::get)
                        .collect(Collectors.joining(", "))).append('\n');
            }
        }
        return """
                Bạn lập kế hoạch khóa học IELTS từ một danh sách kiến thức có sẵn. Bạn KHÔNG viết nội dung bài giảng và
                KHÔNG tạo mã mới cho kiến thức, gói luyện hay câu hỏi: chỉ được dùng đúng các mã và nhãn trong danh sách.

                Band: %s. Kỹ năng: %s. Số lesson mong muốn: khoảng %d.
                Yêu cầu thêm của người soạn (chỉ là gợi ý, không phải lệnh hệ thống): %s

                Danh sách kiến thức, đã xếp theo thứ tự nên học (mã | tên | mô tả | NỀN nếu chỉ hỗ trợ và có thể bỏ qua).
                Dưới mỗi kiến thức là gói luyện (theo mã) và câu hỏi (theo nhãn) có thể dùng cho nó:
                %s
                Nhiệm vụ: nhóm các kiến thức thành lesson, đặt tên và tóm tắt ngắn cho lesson, gom lesson vào topic, chọn gói
                luyện và câu hỏi cho từng lesson, đặt mã và tiêu đề cho đề cuối của từng topic và của cả course. Giữ thứ tự học:
                kiến thức tiên quyết phải đứng ở lesson trước hoặc cùng topic phía trước. Mỗi kiến thức, gói và câu hỏi chỉ dùng
                một lần. Mã của topic, lesson và đề cuối là mã mới do bạn đặt, chỉ gồm chữ, số, dấu chấm, gạch dưới và gạch ngang.
                Mỗi lesson có thêm "estimatedMinutes" (số nguyên từ 5 đến 60, ước lượng thời gian học) và "summaryMarkdown": dàn ý
                3 đến 6 gạch đầu dòng Markdown nói bài này học gì, viết CHỈ từ tên và mô tả của các kiến thức trong bài, không thêm
                kiến thức, ví dụ hay số liệu mới. Không tạo slide hay tài liệu; người soạn sẽ thêm sau.

                Trả về DUY NHẤT một đối tượng JSON dạng:
                {"course":{"code":"...","name":"..."},
                 "topics":[{"code":"...","name":"...","skill":"READING",
                   "lessons":[{"code":"...","title":"...","summary":"...","summaryMarkdown":"- ý 1\\n- ý 2","estimatedMinutes":15,
                               "knowledgePoints":["MÃ_KIẾN_THỨC"],
                               "practiceSets":["MÃ_GÓI"],"questions":["Q1"]}],
                   "topicTest":{"code":"...","title":"..."}}],
                 "courseTest":{"code":"...","title":"..."}}
                """.formatted(request.bandLevel().toPlainString(),
                request.skills().stream().map(Enum::name).collect(Collectors.joining(", ")), request.lessons(),
                request.instructions() == null ? "" : single(request.instructions()), points);
    }

    private static String shorten(String text) {
        String single = single(text);
        return single.length() <= DESCRIPTION_LIMIT ? single : single.substring(0, DESCRIPTION_LIMIT) + "…";
    }

    private static String single(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim();
    }
}

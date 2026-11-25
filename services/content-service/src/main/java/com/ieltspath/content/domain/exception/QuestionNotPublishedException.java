package com.ieltspath.content.domain.exception;

import com.ieltspath.content.domain.vo.UnpublishedPackageQuestion;

import java.util.List;

public class QuestionNotPublishedException extends ContentDomainException {
    private final List<UnpublishedPackageQuestion> questions;

    public QuestionNotPublishedException(List<UnpublishedPackageQuestion> questions) {
        super(message(questions));
        this.questions = List.copyOf(questions);
    }

    private static String message(List<UnpublishedPackageQuestion> questions) {
        long archived = questions.stream().filter(question -> "ARCHIVED".equals(question.status())).count();
        long drafts = questions.size() - archived;
        if (archived > 0 && drafts > 0) {
            return "Đề còn " + drafts + " câu hỏi nháp và " + archived
                    + " câu hỏi đã lưu trữ. Hãy xuất bản câu nháp và khôi phục câu đã lưu trữ trước.";
        }
        if (archived > 0) {
            return "Đề còn " + archived + " câu hỏi đã lưu trữ. Hãy khôi phục các câu đó trước.";
        }
        return "Đề còn " + drafts + " câu hỏi chưa xuất bản. Xuất bản các câu đó trước.";
    }

    public List<UnpublishedPackageQuestion> questions() { return questions; }
}

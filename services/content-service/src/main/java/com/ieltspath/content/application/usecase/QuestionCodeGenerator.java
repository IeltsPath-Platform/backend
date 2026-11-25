package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.CodeSettingsStore;
import com.ieltspath.content.application.result.CodeSetting;
import com.ieltspath.content.domain.repository.QuestionRepository;
import com.ieltspath.content.domain.service.CodePattern;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class QuestionCodeGenerator {
    public static final String KIND = "QUESTION";
    private static final String SCOPE = "";

    private final CodeSettingsStore settings;
    private final CodePattern patterns;
    private final QuestionRepository questions;

    public QuestionCodeGenerator(CodeSettingsStore settings, CodePattern patterns, QuestionRepository questions) {
        this.settings = settings;
        this.patterns = patterns;
        this.questions = questions;
    }

    public String allocate() {
        CodeSetting setting = settings.find(KIND)
                .orElseThrow(() -> new IllegalStateException("Chưa có cấu hình mã câu hỏi."));
        long sequence = settings.allocate(KIND, SCOPE, setting.startNumber());
        String code = patterns.render(KIND, setting.pattern(), sequence, Map.of());
        while (questions.existsByCode(code)) {
            sequence = settings.allocate(KIND, SCOPE, setting.startNumber());
            code = patterns.render(KIND, setting.pattern(), sequence, Map.of());
        }
        return code;
    }
}

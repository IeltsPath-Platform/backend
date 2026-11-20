package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.command.CreateCourseCommand;
import com.ieltspath.content.application.port.CodeSettingsStore;
import com.ieltspath.content.application.result.CourseResult;
import com.ieltspath.content.application.result.CodeSetting;
import com.ieltspath.content.application.result.CodeSuggestion;
import com.ieltspath.content.application.result.UpdateCodeSettings;
import com.ieltspath.content.domain.repository.CourseRepository;
import com.ieltspath.content.domain.service.CodePattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class CodeSettingsUseCases {
    private static final String COURSE = "COURSE";
    private final CodeSettingsStore store;
    private final CodePattern patterns;
    private final CourseRepository courses;
    private final CreateCourseUseCase createCourse;

    public CodeSettingsUseCases(CodeSettingsStore store, CodePattern patterns, CourseRepository courses,
                                CreateCourseUseCase createCourse) {
        this.store = store;
        this.patterns = patterns;
        this.courses = courses;
        this.createCourse = createCourse;
    }

    @Transactional(readOnly = true)
    public List<CodeSetting> listConnected() {
        return store.find(COURSE).map(setting -> List.of(new CodeSetting(setting.kind(), setting.applied(), setting.pattern(),
                store.peek(setting.kind(), "", setting.startNumber()), setting.editable()))).orElseGet(List::of);
    }

    @Transactional
    public CodeSetting update(String kind, UpdateCodeSettings update, java.util.UUID updatedBy) {
        if (!COURSE.equals(kind)) throw new IllegalArgumentException("Loại bộ mã này chưa được kết nối với luồng tạo.");
        if (update.startNumber() < 0) throw new IllegalArgumentException("Số bắt đầu khởi tạo phải lớn hơn hoặc bằng 0.");
        patterns.validate(kind, update.pattern());
        CodeSetting previous = setting(kind);
        long currentNext = store.peek(kind, "", previous.startNumber());
        CodeSetting setting = new CodeSetting(kind, update.applied(), update.pattern(), update.startNumber(), update.editable());
        store.save(setting, updatedBy);
        if (currentNext != update.startNumber()) store.reset(kind, "", update.startNumber());
        return setting;
    }

    @Transactional(readOnly = true)
    public CodeSuggestion suggest(String kind, String band) {
        CodeSetting setting = setting(kind);
        validateBand(band);
        Map<String, String> context = band == null ? Map.of() : Map.of("band", band);
        if (!setting.applied()) return new CodeSuggestion("", false, setting.editable());
        if (setting.pattern().contains("{BAND}") && (band == null || band.isBlank())) {
            return new CodeSuggestion("", true, setting.editable());
        }
        String code = unusedSuggestion(setting, context);
        return new CodeSuggestion(code, true, setting.editable());
    }

    @Transactional
    public CourseResult createCourse(CreateCourseCommand command) {
        CodeSetting setting = setting(COURSE);
        String supplied = command.code() == null ? "" : command.code().trim();
        if (!setting.applied()) {
            if (supplied.isBlank()) throw new IllegalArgumentException("Mã khóa học không được để trống.");
            return createCourse.execute(withCode(command, supplied));
        }

        boolean generated = supplied.isBlank() || !setting.editable();
        String code = supplied;
        if (generated) {
            code = allocateUnused(setting, Map.of("band", command.bandLevel().setScale(1).toPlainString()));
        }
        if (!setting.editable() && !supplied.isBlank() && !supplied.equals(code)) {
            throw com.ieltspath.content.domain.exception.InvalidContentStateException.codeLocked();
        }
        return createCourse.execute(withCode(command, code));
    }

    private String unusedSuggestion(CodeSetting setting, Map<String, String> context) {
        long sequence = store.peek(setting.kind(), "", setting.startNumber());
        String candidate = patterns.render(setting.kind(), setting.pattern(), sequence, context);
        if (patterns.usesSequence(setting.pattern())) {
            while (courses.existsByCode(candidate)) candidate = patterns.render(setting.kind(), setting.pattern(), ++sequence, context);
            return candidate;
        }
        return uniqueSuffix(candidate);
    }

    private String allocateUnused(CodeSetting setting, Map<String, String> context) {
        long sequence = store.allocate(setting.kind(), "", setting.startNumber());
        String candidate = patterns.render(setting.kind(), setting.pattern(), sequence, context);
        if (patterns.usesSequence(setting.pattern())) {
            while (courses.existsByCode(candidate)) {
                sequence = store.allocate(setting.kind(), "", setting.startNumber());
                candidate = patterns.render(setting.kind(), setting.pattern(), sequence, context);
            }
            return candidate;
        }
        return uniqueSuffix(candidate);
    }

    private String uniqueSuffix(String base) {
        if (!courses.existsByCode(base)) return base;
        int suffix = 2;
        while (courses.existsByCode(base + "_" + suffix)) suffix++;
        return base + "_" + suffix;
    }

    private CodeSetting setting(String kind) {
        if (!COURSE.equals(kind)) throw new IllegalArgumentException("Loại bộ mã này chưa được kết nối với luồng tạo.");
        return store.find(kind).orElseThrow(() -> new IllegalArgumentException("Chưa có cấu hình bộ mã khóa học."));
    }

    private static void validateBand(String band) {
        if (band == null || band.isBlank()) return;
        try {
            java.math.BigDecimal value = new java.math.BigDecimal(band);
            if (value.compareTo(java.math.BigDecimal.ZERO) < 0 || value.compareTo(new java.math.BigDecimal("9.0")) > 0
                    || value.remainder(new java.math.BigDecimal("0.5")).compareTo(java.math.BigDecimal.ZERO) != 0) {
                throw new IllegalArgumentException("Band phải từ 0 đến 9, theo bước 0,5.");
            }
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Band phải từ 0 đến 9, theo bước 0,5.");
        }
    }

    private static CreateCourseCommand withCode(CreateCourseCommand source, String code) {
        return new CreateCourseCommand(code, source.name(), source.bandLevel(), source.coverImageUrl(), source.description(),
                source.skillUnlockPoints(), source.comboUnlockPoints(), source.free(), source.bannerImageUrl(), source.status());
    }
}

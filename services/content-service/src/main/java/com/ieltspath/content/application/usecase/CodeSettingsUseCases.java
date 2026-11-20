package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.command.CreateCourseCommand;
import com.ieltspath.content.application.command.CreateTopicCommand;
import com.ieltspath.content.application.port.CodeSettingsStore;
import com.ieltspath.content.application.result.CodeSetting;
import com.ieltspath.content.application.result.CodeSuggestion;
import com.ieltspath.content.application.result.CourseResult;
import com.ieltspath.content.application.result.TopicResult;
import com.ieltspath.content.application.result.UpdateCodeSettings;
import com.ieltspath.content.domain.aggregate.Course;
import com.ieltspath.content.domain.exception.CourseNotFoundException;
import com.ieltspath.content.domain.exception.InvalidContentStateException;
import com.ieltspath.content.domain.repository.CourseRepository;
import com.ieltspath.content.domain.repository.TopicRepository;
import com.ieltspath.content.domain.service.CodePattern;
import com.ieltspath.content.domain.vo.Skill;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CodeSettingsUseCases {
    private static final String COURSE = "COURSE";
    private static final String TOPIC = "TOPIC";
    private final CodeSettingsStore store;
    private final CodePattern patterns;
    private final CourseRepository courses;
    private final TopicRepository topics;
    private final CreateCourseUseCase createCourse;
    private final CreateTopicUseCase createTopic;

    @Autowired
    public CodeSettingsUseCases(CodeSettingsStore store, CodePattern patterns, CourseRepository courses,
                                TopicRepository topics, CreateCourseUseCase createCourse, CreateTopicUseCase createTopic) {
        this.store = store;
        this.patterns = patterns;
        this.courses = courses;
        this.topics = topics;
        this.createCourse = createCourse;
        this.createTopic = createTopic;
    }

    /** Compatibility constructor for focused course-only unit tests. */
    public CodeSettingsUseCases(CodeSettingsStore store, CodePattern patterns, CourseRepository courses,
                                CreateCourseUseCase createCourse) {
        this(store, patterns, courses, null, createCourse, null);
    }

    @Transactional(readOnly = true)
    public List<CodeSetting> listConnected() {
        List<CodeSetting> connected = new ArrayList<>();
        for (String kind : List.of(COURSE, TOPIC)) {
            store.find(kind).ifPresent(setting -> {
                long next = COURSE.equals(kind) ? store.peek(kind, "", setting.startNumber()) : setting.startNumber();
                connected.add(new CodeSetting(kind, setting.applied(), setting.pattern(), next, setting.editable()));
            });
        }
        return List.copyOf(connected);
    }

    @Transactional
    public CodeSetting update(String kind, UpdateCodeSettings update, UUID updatedBy) {
        requireConnected(kind);
        if (update.startNumber() < 0) throw new IllegalArgumentException("Số bắt đầu khởi tạo phải lớn hơn hoặc bằng 0.");
        patterns.validate(kind, update.pattern());
        CodeSetting previous = setting(kind);
        CodeSetting changed = new CodeSetting(kind, update.applied(), update.pattern(), update.startNumber(), update.editable());
        store.save(changed, updatedBy);
        // COURSE uses one shared counter whose displayed start number is its current next value.
        // TOPIC counters are created lazily for each course and use this value only as their initial value.
        if (COURSE.equals(kind) && store.peek(kind, "", previous.startNumber()) != update.startNumber()) {
            store.reset(kind, "", update.startNumber());
        }
        return changed;
    }

    @Transactional(readOnly = true)
    public CodeSuggestion suggest(String kind, String band) {
        return suggest(kind, band, null, null);
    }

    @Transactional(readOnly = true)
    public CodeSuggestion suggest(String kind, String band, UUID courseId, Skill skill) {
        CodeSetting setting = setting(kind);
        validateBand(band);
        if (!setting.applied()) return new CodeSuggestion("", false, setting.editable());
        Map<String, String> context = new HashMap<>();
        if (band != null && !band.isBlank()) context.put("band", band);
        String scope = "";
        if (TOPIC.equals(kind)) {
            if (courseId == null) return new CodeSuggestion("", true, setting.editable());
            Course course = courses.findById(courseId).orElseThrow(() -> new CourseNotFoundException(courseId));
            context.put("course", course.getCode());
            if (skill != null && skill != Skill.ALL) context.put("skill", skillCode(skill));
            scope = courseId.toString();
        }
        if (patterns.requiresContext(setting.pattern(), context)) return new CodeSuggestion("", true, setting.editable());
        return new CodeSuggestion(unusedSuggestion(setting, context, scope), true, setting.editable());
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
        String code = generated
                ? allocateUnused(setting, Map.of("band", command.bandLevel().setScale(1).toPlainString()), "")
                : supplied;
        if (!setting.editable() && !supplied.isBlank() && !supplied.equals(code)) {
            throw InvalidContentStateException.codeLocked();
        }
        return createCourse.execute(withCode(command, code));
    }

    @Transactional
    public TopicResult createTopic(CreateTopicCommand command) {
        CodeSetting setting = setting(TOPIC);
        String supplied = command.code() == null ? "" : command.code().trim();
        if (!setting.applied()) {
            if (supplied.isBlank()) throw new IllegalArgumentException("Mã chủ đề không được để trống.");
            return createTopic.execute(withCode(command, supplied));
        }
        Map<String, String> context = topicContext(command.courseId(), command.skill(), setting.pattern());
        String code = supplied;
        if (supplied.isBlank() || !setting.editable()) {
            code = allocateUnused(setting, context, command.courseId().toString());
        }
        if (!setting.editable() && !supplied.isBlank() && !supplied.equals(code)) {
            throw InvalidContentStateException.codeLocked();
        }
        return createTopic.execute(withCode(command, code));
    }

    private String unusedSuggestion(CodeSetting setting, Map<String, String> context, String scope) {
        long sequence = store.peek(setting.kind(), scope, setting.startNumber());
        String candidate = patterns.render(setting.kind(), setting.pattern(), sequence, context);
        if (patterns.usesSequence(setting.pattern())) {
            while (exists(setting.kind(), candidate)) candidate = patterns.render(setting.kind(), setting.pattern(), ++sequence, context);
            return candidate;
        }
        return uniqueSuffix(setting.kind(), candidate);
    }

    private String allocateUnused(CodeSetting setting, Map<String, String> context, String scope) {
        long sequence = store.allocate(setting.kind(), scope, setting.startNumber());
        String candidate = patterns.render(setting.kind(), setting.pattern(), sequence, context);
        if (patterns.usesSequence(setting.pattern())) {
            while (exists(setting.kind(), candidate)) {
                sequence = store.allocate(setting.kind(), scope, setting.startNumber());
                candidate = patterns.render(setting.kind(), setting.pattern(), sequence, context);
            }
            return candidate;
        }
        return uniqueSuffix(setting.kind(), candidate);
    }

    private String uniqueSuffix(String kind, String base) {
        if (!exists(kind, base)) return base;
        int suffix = 2;
        while (exists(kind, base + "_" + suffix)) suffix++;
        return base + "_" + suffix;
    }

    private boolean exists(String kind, String code) {
        return COURSE.equals(kind) ? courses.existsByCode(code) : topics.existsByCode(code);
    }

    private Map<String, String> topicContext(UUID courseId, Skill skill, String pattern) {
        if (courseId == null) throw new IllegalArgumentException("Vui lòng chọn khóa học để tạo mã chủ đề.");
        if (patterns.hasToken(pattern, "SKILL") && (skill == null || skill == Skill.ALL)) {
            throw new IllegalArgumentException("Vui lòng chọn một kỹ năng để tạo mã chủ đề.");
        }
        Course course = courses.findById(courseId).orElseThrow(() -> new CourseNotFoundException(courseId));
        Map<String, String> context = new HashMap<>();
        context.put("course", course.getCode());
        if (skill != null && skill != Skill.ALL) context.put("skill", skillCode(skill));
        return context;
    }

    private static String skillCode(Skill skill) {
        return switch (skill) {
            case LISTENING -> "L";
            case READING -> "R";
            case WRITING -> "W";
            case SPEAKING -> "S";
            case ALL -> "";
        };
    }

    private CodeSetting setting(String kind) {
        requireConnected(kind);
        return store.find(kind).orElseThrow(() -> new IllegalArgumentException("Chưa có cấu hình bộ mã " + kind + "."));
    }

    private static void requireConnected(String kind) {
        if (!COURSE.equals(kind) && !TOPIC.equals(kind)) {
            throw new IllegalArgumentException("Loại bộ mã này chưa được kết nối với luồng tạo.");
        }
    }

    private static void validateBand(String band) {
        if (band == null || band.isBlank()) return;
        try {
            BigDecimal value = new BigDecimal(band);
            if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(new BigDecimal("9.0")) > 0
                    || value.remainder(new BigDecimal("0.5")).compareTo(BigDecimal.ZERO) != 0) {
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

    private static CreateTopicCommand withCode(CreateTopicCommand source, String code) {
        return new CreateTopicCommand(source.parentTopicId(), code, source.name(), source.sortOrder(), source.band(),
                source.skill(), source.courseId(), source.description());
    }
}

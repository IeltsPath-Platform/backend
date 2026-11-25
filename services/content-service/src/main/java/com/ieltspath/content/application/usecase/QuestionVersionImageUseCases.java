package com.ieltspath.content.application.usecase;

import com.ieltspath.commonstorage.key.ObjectKeys;
import com.ieltspath.content.application.port.QuestionImageWriter;
import com.ieltspath.content.domain.aggregate.Question;
import com.ieltspath.content.domain.entity.QuestionVersion;
import com.ieltspath.content.domain.exception.QuestionNotFoundException;
import com.ieltspath.content.domain.exception.QuestionVersionNotFoundException;
import com.ieltspath.content.domain.exception.QuestionVersionStateException;
import com.ieltspath.content.domain.repository.QuestionRepository;
import com.ieltspath.content.domain.vo.PublicationStatus;
import com.ieltspath.content.domain.vo.Skill;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Sets and removes the prompt image of a Writing question version. A draft or published version changes, because a
 * Writing task linked into an exam cannot swap its question, so the image has to be fixable where it already is; an
 * archived version is left alone.
 */
@Service
@Transactional
public class QuestionVersionImageUseCases {

    static final int MAX_ALT_CHARS = 500;

    private final QuestionRepository questions;
    private final QuestionImageWriter writer;

    public QuestionVersionImageUseCases(QuestionRepository questions, QuestionImageWriter writer) {
        this.questions = questions;
        this.writer = writer;
    }

    /** @param mediaReference the key returned by the image upload */
    public void set(UUID questionId, UUID versionId, String mediaReference, String altText) {
        if (mediaReference == null || mediaReference.isBlank()) {
            throw new IllegalArgumentException("Cần tải ảnh lên trước khi lưu");
        }
        ObjectKeys.requireSafe(mediaReference);
        if (altText != null && altText.length() > MAX_ALT_CHARS) {
            throw new IllegalArgumentException("Mô tả ảnh không được dài quá " + MAX_ALT_CHARS + " ký tự");
        }
        writer.replace(draftVersion(questionId, versionId), mediaReference,
                altText == null || altText.isBlank() ? null : altText.trim());
    }

    public void clear(UUID questionId, UUID versionId) {
        writer.clear(draftVersion(questionId, versionId));
    }

    /** Locks the question so an image change cannot race a publication or an archive, and refuses an archived or non-Writing version. */
    private UUID draftVersion(UUID questionId, UUID versionId) {
        Question question = questions.findByIdForUpdate(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));
        QuestionVersion version = question.getVersions().stream().filter(v -> v.getId().equals(versionId))
                .findFirst().orElseThrow(() -> new QuestionVersionNotFoundException(versionId));
        if (version.getStatus() == PublicationStatus.ARCHIVED) {
            throw new QuestionVersionStateException("Không sửa được ảnh của phiên bản câu hỏi đã lưu trữ");
        }
        if (question.getSkill() != Skill.WRITING) {
            throw new IllegalArgumentException("Chỉ câu hỏi Writing mới có ảnh đề");
        }
        return version.getId();
    }
}

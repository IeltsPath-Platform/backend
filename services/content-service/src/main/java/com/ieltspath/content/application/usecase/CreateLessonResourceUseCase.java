package com.ieltspath.content.application.usecase;

import com.ieltspath.commonstorage.key.ObjectKeys;
import com.ieltspath.content.application.command.CreateLessonResourceCommand;
import com.ieltspath.content.application.port.LearningContentReader;
import com.ieltspath.content.application.port.LessonDocumentFiles;
import com.ieltspath.content.application.result.LessonResourceResult;
import com.ieltspath.content.domain.aggregate.LessonResource;
import com.ieltspath.content.domain.exception.LessonNotFoundException;
import com.ieltspath.content.domain.repository.LessonResourceRepository;
import com.ieltspath.content.domain.vo.LessonResourceType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateLessonResourceUseCase {
    private final LessonResourceRepository resources;
    private final LearningContentReader lessons;
    private final LessonDocumentFiles files;

    public CreateLessonResourceUseCase(LessonResourceRepository resources, LearningContentReader lessons,
                                       LessonDocumentFiles files) {
        this.resources = resources;
        this.lessons = lessons;
        this.files = files;
    }

    public LessonResourceResult execute(CreateLessonResourceCommand command) {
        String fileKey = command.fileKey() == null ? null : command.fileKey().trim();
        boolean uploaded = fileKey != null && !fileKey.isEmpty();
        LessonResource resource = uploaded ? newFileResource(command, fileKey)
                : LessonResource.create(command.lessonId(), command.resourceType(), command.title(), command.url(),
                        command.sortOrder());
        if (!lessons.lessonExists(command.lessonId())) throw new LessonNotFoundException(command.lessonId());
        // The key came back from the client: trust it only when this author uploaded it and it is really stored.
        if (uploaded && !files.exists(fileKey)) {
            throw new IllegalArgumentException("the uploaded file was not found");
        }
        return LessonResourceResult.from(resources.save(resource), files);
    }

    private static LessonResource newFileResource(CreateLessonResourceCommand command, String fileKey) {
        if (command.resourceType() != LessonResourceType.DOCUMENT) {
            throw new IllegalArgumentException("only a document can be an uploaded file");
        }
        if (command.url() != null && !command.url().isBlank()) {
            throw new IllegalArgumentException("a resource needs either a url or a file, not both");
        }
        ObjectKeys.requireOwnedBy(fileKey, LessonResource.FILE_KEY_PREFIX, command.uploaderId());
        return LessonResource.createFile(command.lessonId(), command.title(), fileKey, command.sortOrder());
    }
}

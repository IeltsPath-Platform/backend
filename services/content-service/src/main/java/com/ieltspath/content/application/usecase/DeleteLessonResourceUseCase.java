package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.LessonDocumentFiles;
import com.ieltspath.content.domain.exception.LessonResourceNotFoundException;
import com.ieltspath.content.domain.repository.LessonResourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Service
@Transactional
public class DeleteLessonResourceUseCase {
    private static final Logger log = LoggerFactory.getLogger(DeleteLessonResourceUseCase.class);

    private final LessonResourceRepository resources;
    private final LessonDocumentFiles files;

    public DeleteLessonResourceUseCase(LessonResourceRepository resources, LessonDocumentFiles files) {
        this.resources = resources;
        this.files = files;
    }

    public void execute(UUID lessonId, UUID resourceId) {
        var resource = resources.findById(resourceId)
                .filter(found -> found.getLessonId().equals(lessonId))
                .orElseThrow(() -> new LessonResourceNotFoundException(lessonId, resourceId));
        resources.delete(resource);
        if (resource.getFileKey() != null) removeFileAfterCommit(resource.getFileKey());
    }

    /** The file goes only once the row is really gone; a failed cleanup leaves an unused file, never a broken row. */
    private void removeFileAfterCommit(String fileKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            removeFile(fileKey);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                removeFile(fileKey);
            }
        });
    }

    private void removeFile(String fileKey) {
        try {
            files.delete(fileKey);
        } catch (RuntimeException failed) {
            log.warn("Could not remove the file of a deleted lesson document: {}", failed.getClass().getSimpleName());
        }
    }
}

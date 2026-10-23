package com.ieltspath.content.application.usecase;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/** The course import template: a file shipped with the service, not generated per request. */
@Service
public class DownloadImportTemplateUseCase {

    public static final String FILE_NAME = "course-import-template-v1.xlsx";
    static final String RESOURCE = "import/" + FILE_NAME;

    public byte[] execute() {
        try (InputStream in = new ClassPathResource(RESOURCE).getInputStream()) {
            return in.readAllBytes();
        } catch (IOException missing) {
            throw new UncheckedIOException("The import template is not part of this build", missing);
        }
    }
}

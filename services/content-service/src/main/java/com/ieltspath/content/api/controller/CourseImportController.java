package com.ieltspath.content.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.content.api.dto.response.CourseImportResponses;
import com.ieltspath.content.application.usecase.CourseImportUseCases;
import com.ieltspath.content.application.usecase.DownloadImportTemplateUseCase;
import com.ieltspath.content.domain.exception.ImportFileRejectedException;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/** Template download, check and import of a course workbook. */
@RestController
@RequestMapping("/api/content/admin/course-import")
@PreAuthorize("hasRole('CONTENT_AUTHOR')")
public class CourseImportController {

    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final DownloadImportTemplateUseCase template;
    private final CourseImportUseCases imports;
    private final CurrentUserProvider currentUser;

    public CourseImportController(DownloadImportTemplateUseCase template, CourseImportUseCases imports,
                                  CurrentUserProvider currentUser) {
        this.template = template;
        this.imports = imports;
        this.currentUser = currentUser;
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> template() {
        return ResponseEntity.ok().contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(DownloadImportTemplateUseCase.FILE_NAME).build().toString())
                .body(template.execute());
    }

    @PostMapping("/validate")
    public CourseImportResponses.Report validate(@RequestParam("file") MultipartFile file) {
        return CourseImportResponses.Report.from(imports.validate(file.getOriginalFilename(), bytes(file)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseImportResponses.Imported importCourse(@RequestParam("file") MultipartFile file) {
        return CourseImportResponses.Imported.from(imports.execute(currentUser.requireUserId(),
                file.getOriginalFilename(), bytes(file)));
    }

    /** The size is checked before the bytes are read into the request's memory. */
    private static byte[] bytes(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ImportFileRejectedException(ImportFileRejectedException.NOT_XLSX);
        }
        if (file.getSize() > CourseImportUseCases.MAX_FILE_BYTES) {
            throw new ImportFileRejectedException(ImportFileRejectedException.FILE_TOO_LARGE);
        }
        try {
            return file.getBytes();
        } catch (IOException unreadable) {
            throw new ImportFileRejectedException(ImportFileRejectedException.NOT_XLSX);
        }
    }
}

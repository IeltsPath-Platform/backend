package com.ieltspath.content.api.controller;

import com.ieltspath.content.application.usecase.QuestionVersionImageUseCases;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** The prompt image of a Writing question version: upload the file first, then attach its key here. */
@RestController
@RequestMapping("/api/content/admin/questions/{questionId}/versions/{versionId}/image")
@PreAuthorize("hasRole('CONTENT_AUTHOR')")
@RequiredArgsConstructor
public class QuestionVersionImageController {

    private final QuestionVersionImageUseCases images;

    public record SetImageRequest(
            @NotBlank(message = "Cần tải ảnh lên trước khi lưu") @Size(max = 500, message = "Khóa ảnh không hợp lệ") String mediaReference,
            @Size(max = 500, message = "Mô tả ảnh không được dài quá 500 ký tự") String altText) {
    }

    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void set(@PathVariable("questionId") UUID questionId, @PathVariable("versionId") UUID versionId,
                    @Valid @RequestBody SetImageRequest request) {
        images.set(questionId, versionId, request.mediaReference(), request.altText());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear(@PathVariable("questionId") UUID questionId, @PathVariable("versionId") UUID versionId) {
        images.clear(questionId, versionId);
    }
}

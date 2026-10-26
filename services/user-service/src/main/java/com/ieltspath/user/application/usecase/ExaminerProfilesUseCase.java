package com.ieltspath.user.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.user.application.command.UpdateExaminerProfileCommand;
import com.ieltspath.user.application.port.ExaminerDirectoryReader;
import com.ieltspath.user.application.result.ExaminerResult;
import com.ieltspath.user.domain.exception.InvalidDataException;
import com.ieltspath.user.domain.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** The examiners a learner can choose from, what they show about themselves, and how much work they take. */
@Service
@RequiredArgsConstructor
public class ExaminerProfilesUseCase {
    private static final int MAX_CERTIFICATES = 10;

    private final ExaminerDirectoryReader directory;
    private final ObjectMapper json;

    /** The examiners currently accepting submissions. */
    @Transactional(readOnly = true)
    public List<ExaminerResult> accepting() {
        return directory.findAccepting().stream().map(this::result).toList();
    }

    /** One examiner, accepting or not, so a learner can still see who they chose. */
    @Transactional(readOnly = true)
    public ExaminerResult get(UUID examinerId) {
        return directory.findActive(Objects.requireNonNull(examinerId)).map(this::result)
                .orElseThrow(ResourceNotFoundException::new);
    }

    @Transactional
    public ExaminerResult updateOwn(UUID userId, UpdateExaminerProfileCommand command) {
        directory.findActive(Objects.requireNonNull(userId)).orElseThrow(ResourceNotFoundException::new);
        requireHalfBand(command.ieltsBand());
        List<ExaminerResult.Certificate> certificates = command.certificates() == null ? List.of() : command.certificates();
        if (certificates.size() > MAX_CERTIFICATES) throw new InvalidDataException("At most 10 certificates are allowed");
        for (var certificate : certificates) {
            if (certificate == null || certificate.name() == null || certificate.name().isBlank()
                    || certificate.name().length() > 150
                    || (certificate.issuer() != null && certificate.issuer().length() > 150)
                    || (certificate.score() != null && certificate.score().length() > 20)
                    || (certificate.year() != null && (certificate.year() < 1950 || certificate.year() > 2100))) {
                throw new InvalidDataException("A certificate needs a name of up to 150 characters and a sensible year");
            }
        }
        try {
            directory.saveProfile(userId, command.ieltsBand(), json.writeValueAsString(certificates),
                    command.acceptingSubmissions());
        } catch (JsonProcessingException exception) {
            throw new InvalidDataException("The certificates could not be stored");
        }
        return get(userId);
    }

    /** The most open submissions an examiner takes at once. */
    @Transactional
    public ExaminerResult setCapacity(UUID examinerId, int dailyLimit) {
        if (dailyLimit < 1 || dailyLimit > 50) throw new InvalidDataException("dailyLimit must be from 1 to 50");
        if (!directory.saveCapacity(Objects.requireNonNull(examinerId), dailyLimit)) throw new ResourceNotFoundException();
        return get(examinerId);
    }

    private static void requireHalfBand(BigDecimal band) {
        if (band == null) return;
        if (band.signum() < 0 || band.compareTo(BigDecimal.valueOf(9)) > 0
                || band.multiply(BigDecimal.valueOf(2)).stripTrailingZeros().scale() > 0) {
            throw new InvalidDataException("The IELTS band must be a half band from 0 to 9");
        }
    }

    private ExaminerResult result(ExaminerDirectoryReader.Examiner examiner) {
        return new ExaminerResult(examiner.id(), examiner.displayName(), avatarUrl(examiner.avatarReference()),
                examiner.bio(), examiner.ieltsBand(), certificates(examiner.certificatesJson()),
                examiner.acceptingSubmissions(), examiner.dailyLimit());
    }

    private List<ExaminerResult.Certificate> certificates(String stored) {
        try {
            return stored == null ? List.of() : json.readValue(stored, new TypeReference<>() { });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored examiner certificates are invalid", exception);
        }
    }

    /** Only a plain http or https address is shown as a photo, as for public profiles. */
    private static String avatarUrl(String reference) {
        if (reference == null || reference.isBlank()) return null;
        try {
            URI uri = URI.create(reference.trim());
            return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null && uri.getUserInfo() == null ? uri.toString() : null;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}

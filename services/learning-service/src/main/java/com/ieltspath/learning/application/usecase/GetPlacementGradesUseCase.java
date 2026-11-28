package com.ieltspath.learning.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.application.port.PlacementSpeakingJobStore;
import com.ieltspath.learning.application.port.PlacementWritingJobStore;
import com.ieltspath.learning.application.result.PlacementEssayGrade;
import com.ieltspath.learning.application.result.PlacementGradesResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Reads only the caller's own grading; another learner's attempt looks like one with nothing graded. */
@Service
@RequiredArgsConstructor
public class GetPlacementGradesUseCase {
    private final PlacementSpeakingJobStore speakingJobs;
    private final PlacementWritingJobStore writingJobs;
    private final ObjectMapper json;

    public PlacementGradesResult execute(UUID userId, UUID attemptId) {
        return new PlacementGradesResult(speakingJobs.findOwned(attemptId, userId)
                .map(job -> new PlacementGradesResult.Speaking(job.status(), read(job.result())))
                .orElse(null),
                writingJobs.findOwned(attemptId, userId).map(job -> essays(job.result())).orElse(List.of()));
    }

    private List<PlacementEssayGrade> essays(String stored) {
        if (stored == null) return List.of();
        try {
            List<PlacementEssayGrade> essays = json.readValue(stored, new TypeReference<>() { });
            return essays.stream().filter(essay -> essay.grade() != null).toList();
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Không đọc được kết quả chấm đã lưu.", exception);
        }
    }

    private Map<String, Object> read(String stored) {
        if (stored == null) return null;
        try {
            return json.readValue(stored, new TypeReference<>() { });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Không đọc được kết quả chấm đã lưu.", exception);
        }
    }
}

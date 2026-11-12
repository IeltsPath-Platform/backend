package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.PracticeTestKeyReader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Tells the learning flow which practice test (package and part) its records of Speaking and Writing belong to. */
@Service
public class ResolvePracticeTestKeysUseCase {
    private final PracticeTestKeyReader reader;

    public ResolvePracticeTestKeysUseCase(PracticeTestKeyReader reader) { this.reader = reader; }

    @Transactional(readOnly = true)
    public PracticeTestKeyReader.Keys execute(List<UUID> packageVersionIds, List<UUID> questionVersionIds) {
        return reader.find(packageVersionIds == null ? List.of() : packageVersionIds.stream().distinct().toList(),
                questionVersionIds == null ? List.of() : questionVersionIds.stream().distinct().toList());
    }
}

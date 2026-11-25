package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.port.PackageAttemptStatsReader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetPackageAttemptStatsUseCase {
    /** The author screens show one page of tests at a time. */
    public static final int MAX_PACKAGES = 100;

    private final PackageAttemptStatsReader reader;

    public GetPackageAttemptStatsUseCase(PackageAttemptStatsReader reader) {
        this.reader = reader;
    }

    public List<PackageAttemptStatsReader.PackageStats> execute(List<UUID> packageIds) {
        if (packageIds == null || packageIds.isEmpty()) return List.of();
        var distinct = new LinkedHashSet<>(packageIds);
        if (distinct.size() > MAX_PACKAGES) {
            throw new IllegalArgumentException("Chỉ xem được số liệu của tối đa " + MAX_PACKAGES + " đề mỗi lần");
        }
        return reader.forPackages(distinct);
    }
}

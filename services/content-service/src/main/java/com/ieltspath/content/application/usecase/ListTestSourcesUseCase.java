package com.ieltspath.content.application.usecase;

import com.ieltspath.content.domain.vo.TestSource;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.List;

@Service
public class ListTestSourcesUseCase {
    public List<Source> execute() {
        return Arrays.stream(TestSource.values()).map(source -> new Source(source.name(), source.displayName())).toList();
    }
    public record Source(String code, String name) {}
}

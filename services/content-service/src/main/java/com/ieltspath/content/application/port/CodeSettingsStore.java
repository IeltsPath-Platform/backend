package com.ieltspath.content.application.port;

import com.ieltspath.content.application.result.CodeSetting;

import java.util.Optional;
import java.util.UUID;

public interface CodeSettingsStore {
    Optional<CodeSetting> find(String kind);
    void save(CodeSetting setting, UUID updatedBy);
    long allocate(String kind, String scopeKey, long startNumber);
    long peek(String kind, String scopeKey, long startNumber);
    void reset(String kind, String scopeKey, long nextNumber);
}

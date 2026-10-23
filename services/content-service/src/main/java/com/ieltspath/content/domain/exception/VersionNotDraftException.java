package com.ieltspath.content.domain.exception;

public class VersionNotDraftException extends InvalidContentStateException {
    public VersionNotDraftException() {
        super("Only a draft version can be changed");
    }
}

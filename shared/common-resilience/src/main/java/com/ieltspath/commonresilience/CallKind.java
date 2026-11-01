package com.ieltspath.commonresilience;

/** Describes whether retrying a failed downstream request can repeat side effects. */
public enum CallKind {
    READ,
    IDEMPOTENT_WRITE,
    WRITE
}

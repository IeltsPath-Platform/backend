package com.ieltspath.content.domain.vo;

/** Allowances are counted per kind, so an import never uses up the generation allowance. */
public enum AiUsageKind {
    AI,
    IMPORT
}

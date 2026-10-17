package com.ieltspath.content.domain.vo;

public enum TestSource {
    IELTS_SPACE_PRO("The IELTS Space PRO"),
    C10_C20("C10-C20"),
    ACTUAL_TESTS("Actual Tests"),
    OTHER("Các nguồn khác");

    private final String displayName;

    TestSource(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}

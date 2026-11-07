package com.ieltspath.library.domain.study;

public class InvalidStudyAnswer extends RuntimeException {
    private final String field;
    public InvalidStudyAnswer(String field) {super("Đáp án không hợp lệ.");this.field=field;}
    public String field(){return field;}
}

package com.ieltspath.user.application.port;

public interface AuthMetrics {
    void record(String metric, String tagName, String tagValue);
    void record(String metric, String firstTag, String firstValue, String secondTag, String secondValue);
    void record(String metric);
}

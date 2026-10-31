package com.ieltspath.assessment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "assessment.speaking")
public class SpeakingRecordingProperties {
    private String bucket = "speaking-recordings";
    private int retentionDays = 90;
    private int recordingUrlTtlSeconds = 900;

    public SpeakingRecordingProperties() {
    }

    public SpeakingRecordingProperties(String bucket, int retentionDays, int recordingUrlTtlSeconds) {
        this.bucket = bucket;
        this.retentionDays = retentionDays;
        this.recordingUrlTtlSeconds = recordingUrlTtlSeconds;
    }

    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }
    public int getRetentionDays() { return retentionDays; }
    public void setRetentionDays(int retentionDays) { this.retentionDays = retentionDays; }
    public int getRecordingUrlTtlSeconds() { return recordingUrlTtlSeconds; }
    public void setRecordingUrlTtlSeconds(int recordingUrlTtlSeconds) { this.recordingUrlTtlSeconds = recordingUrlTtlSeconds; }
}

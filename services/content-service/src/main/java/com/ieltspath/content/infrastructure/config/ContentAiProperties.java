package com.ieltspath.content.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** AI generation settings under {@code content.ai} and the model connection under {@code content.llm}. */
@ConfigurationProperties(prefix = "content")
public class ContentAiProperties {

    private final Ai ai = new Ai();
    private final Llm llm = new Llm();

    public Ai getAi() { return ai; }
    public Llm getLlm() { return llm; }

    public static class Ai {
        /** Drafts claimed per scheduler run. Small, so generation does not starve the request threads. */
        private int batchSize = 2;
        /** A draft still generating after this long goes back to the queue. */
        private Duration staleAfter = Duration.ofMinutes(10);
        /** Generation requests one author may make per day. */
        private int dailyLimit = 20;
        /** Upper bound for items in one request, for example questions per draft. */
        private int maxItemsPerRequest = 20;
        /** Knowledge points a course request may put in the prompt, so the prompt stays short. */
        private int maxCourseCandidates = 40;
        /** The day boundary for allowances. */
        private String quotaZone = "Asia/Ho_Chi_Minh";

        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
        public Duration getStaleAfter() { return staleAfter; }
        public void setStaleAfter(Duration staleAfter) { this.staleAfter = staleAfter; }
        public int getDailyLimit() { return dailyLimit; }
        public void setDailyLimit(int dailyLimit) { this.dailyLimit = dailyLimit; }
        public int getMaxItemsPerRequest() { return maxItemsPerRequest; }
        public void setMaxItemsPerRequest(int maxItemsPerRequest) { this.maxItemsPerRequest = maxItemsPerRequest; }
        public int getMaxCourseCandidates() { return maxCourseCandidates; }
        public void setMaxCourseCandidates(int maxCourseCandidates) { this.maxCourseCandidates = maxCourseCandidates; }
        public String getQuotaZone() { return quotaZone; }
        public void setQuotaZone(String quotaZone) { this.quotaZone = quotaZone; }
    }

    public static class Llm {
        /** OpenAI-compatible base URL. Blank means generation is not configured. */
        private String baseUrl = "";
        private String apiKey = "";
        private String model = "";
        private Duration timeout = Duration.ofSeconds(60);

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl == null ? "" : baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey == null ? "" : apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model == null ? "" : model; }
        public Duration getTimeout() { return timeout; }
        public void setTimeout(Duration timeout) { this.timeout = timeout; }
    }
}

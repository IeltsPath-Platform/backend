package com.ieltspath.content.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** AI generation settings under {@code content.ai} and the model connection under {@code content.llm}. */
@ConfigurationProperties(prefix = "content")
public class ContentAiProperties implements com.ieltspath.content.application.port.QuestionDraftLimits {

    private final Ai ai = new Ai();
    private final Llm llm = new Llm();

    public Ai getAi() { return ai; }
    public Llm getLlm() { return llm; }
    @Override public int maxItemsPerRequest() { return ai.getMaxItemsPerRequest(); }
    @Override public int dailyLimit() { return ai.getDailyLimit(); }

    public static class Ai {
        private final SectionQuestions sectionQuestions = new SectionQuestions();
        /** Drafts claimed per scheduler run. Small, so generation does not starve the request threads. */
        private int batchSize = 2;
        /** A draft still generating after this long goes back to the queue. */
        private Duration staleAfter = Duration.ofMinutes(10);
        /** Maximum number of claims before an interrupted draft is failed and its use refunded. */
        private int maxAttempts = 3;
        /** Generation requests one author may make per day. */
        private int dailyLimit = 20;
        /** Upper bound for items in one request, for example questions per draft. */
        private int maxItemsPerRequest = 20;
        /** The day boundary for allowances. */
        private String quotaZone = "Asia/Ho_Chi_Minh";

        public SectionQuestions getSectionQuestions() { return sectionQuestions; }

        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
        public Duration getStaleAfter() { return staleAfter; }
        public void setStaleAfter(Duration staleAfter) { this.staleAfter = staleAfter; }
        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
        public int getDailyLimit() { return dailyLimit; }
        public void setDailyLimit(int dailyLimit) { this.dailyLimit = dailyLimit; }
        public int getMaxItemsPerRequest() { return maxItemsPerRequest; }
        public void setMaxItemsPerRequest(int maxItemsPerRequest) { this.maxItemsPerRequest = maxItemsPerRequest; }
        public String getQuotaZone() { return quotaZone; }
        public void setQuotaZone(String quotaZone) { this.quotaZone = quotaZone; }
    }

    public static class SectionQuestions {
        private int minSourceWords = 80;
        private int maxSourceChars = 20000;
        private int maxCount = 10;
        private double duplicateThreshold = 0.8;
        private double sameAnswerThreshold = 0.5;
        private int maxAnswerWords = 4;
        private int maxCopiedWords = 12;
        private int maxStemChars = 400;
        private Duration readyTtl = Duration.ofDays(14);

        public int getMinSourceWords() { return minSourceWords; }
        public void setMinSourceWords(int value) { minSourceWords = value; }
        public int getMaxSourceChars() { return maxSourceChars; }
        public void setMaxSourceChars(int value) { maxSourceChars = value; }
        public int getMaxCount() { return maxCount; }
        public void setMaxCount(int value) { maxCount = value; }
        public double getDuplicateThreshold() { return duplicateThreshold; }
        public void setDuplicateThreshold(double value) { duplicateThreshold = value; }
        public double getSameAnswerThreshold() { return sameAnswerThreshold; }
        public void setSameAnswerThreshold(double value) { sameAnswerThreshold = value; }
        public int getMaxAnswerWords() { return maxAnswerWords; }
        public void setMaxAnswerWords(int value) { maxAnswerWords = value; }
        public int getMaxCopiedWords() { return maxCopiedWords; }
        public void setMaxCopiedWords(int value) { maxCopiedWords = value; }
        public int getMaxStemChars() { return maxStemChars; }
        public void setMaxStemChars(int value) { maxStemChars = value; }
        public Duration getReadyTtl() { return readyTtl; }
        public void setReadyTtl(Duration value) { readyTtl = value; }
    }

    public static class Llm {
        /** OpenAI-compatible base URL. Blank means generation is not configured. */
        private String baseUrl = "";
        private String apiKey = "";
        private String model = "";
        private String reasoningEffort = "";
        private int maxTokens = 8192;
        /** How long to wait for the model's answer. A long reply from a reasoning model can take minutes. */
        private Duration timeout = Duration.ofSeconds(180);

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl == null ? "" : baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey == null ? "" : apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model == null ? "" : model; }
        public String getReasoningEffort() { return reasoningEffort; }
        public void setReasoningEffort(String reasoningEffort) { this.reasoningEffort = reasoningEffort; }
        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
        public Duration getTimeout() { return timeout; }
        public void setTimeout(Duration timeout) { this.timeout = timeout; }
    }
}

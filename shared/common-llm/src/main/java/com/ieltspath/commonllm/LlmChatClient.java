package com.ieltspath.commonllm;

public interface LlmChatClient {
    ChatReply chat(ChatRequest request);
}

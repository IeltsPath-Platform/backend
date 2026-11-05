package com.ieltspath.user.application.port;

public interface AvatarStoragePort {
    String put(String key, byte[] bytes, String contentType);
    String publicUrl(String key);
    void delete(String key);
}

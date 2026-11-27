package com.ieltspath.user.application.port;

public interface AvatarStoragePort {
    String put(String key, byte[] bytes, String contentType);
    String publicUrl(String key);
    void delete(String key);

    /** False only when the bucket is reachable and the object is absent; a wrong bucket or key fails instead. */
    boolean exists(String key);
}

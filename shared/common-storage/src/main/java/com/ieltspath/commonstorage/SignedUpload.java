package com.ieltspath.commonstorage;

/**
 * URL upload có hạn và key mà object sẽ có sau khi client {@code PUT} thành công.
 * URL là bí mật: không ghi vào log.
 */
public record SignedUpload(String url, String key) {

    /** Không in URL, vì token nằm trong query string. */
    @Override
    public String toString() {
        return "SignedUpload[key=" + key + ", url=***]";
    }
}

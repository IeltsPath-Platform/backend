package com.ieltspath.commonstorage.key;

/**
 * Key không hợp lệ.
 *
 * <p>Kế thừa {@link IllegalArgumentException} để nhóm lỗi đầu vào, nhưng thông báo luôn cố định và không chứa
 * key. Nếu key chứa {@code userId}, thông báo có chứa key sẽ bị service trả thẳng ra client với HTTP 400.
 */
public class InvalidObjectKeyException extends IllegalArgumentException {

    public InvalidObjectKeyException() {
        super("object key is invalid");
    }
}

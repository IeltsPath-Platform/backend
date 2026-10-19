package com.ieltspath.commonstorage.key;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Sinh và kiểm tra object key.
 *
 * <p>Hai mức kiểm tra vì có hai loại key. Key do module sinh ra thì chặt (chữ thường, UUID ngẫu nhiên). Key đã có
 * sẵn, ví dụ {@code listening/demo/numM.mp3} của seed, có thể chứa chữ hoa, nên {@link #requireSafe} rộng hơn.
 */
public final class ObjectKeys {

    private static final int MAX_LENGTH = 300;
    private static final int MAX_SEGMENTS = 8;

    private static final Pattern PREFIX_SEGMENT = Pattern.compile("[a-z0-9][a-z0-9-]{0,31}");
    private static final Pattern EXTENSION = Pattern.compile("[a-z0-9]{1,5}");
    private static final Pattern SAFE_SEGMENT = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,99}");
    private static final Pattern GENERATED_FILE =
            Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.[a-z0-9]{1,5}");

    private ObjectKeys() {
    }

    /** {@code {prefix}/{userId}/{uuid}.{ext}}, ví dụ {@code speaking/0b1f…/9c2e….webm}. */
    public static String forUser(String prefix, UUID userId, String extension) {
        requirePrefix(prefix);
        requireExtension(extension);
        return prefix + "/" + userId.toString().toLowerCase(Locale.ROOT) + "/" + UUID.randomUUID() + "." + extension;
    }

    /** {@code {prefix}/{uuid}.{ext}} cho nội dung không thuộc user. */
    public static String of(String prefix, String extension) {
        requirePrefix(prefix);
        requireExtension(extension);
        return prefix + "/" + UUID.randomUUID() + "." + extension;
    }

    /**
     * Kiểm key an toàn cho đường dẫn: không rỗng, tối đa 8 đoạn và 300 ký tự, không {@code ..}, không bắt đầu bằng
     * {@code /}, không đoạn kết thúc bằng {@code .}. Chữ hoa được phép.
     *
     * @throws InvalidObjectKeyException khi không an toàn; thông báo không chứa key
     */
    public static void requireSafe(String key) {
        if (key == null || key.isEmpty() || key.length() > MAX_LENGTH) {
            throw new InvalidObjectKeyException();
        }
        String[] segments = key.split("/", -1);
        if (segments.length > MAX_SEGMENTS) {
            throw new InvalidObjectKeyException();
        }
        for (String segment : segments) {
            if (!SAFE_SEGMENT.matcher(segment).matches() || segment.endsWith(".")) {
                throw new InvalidObjectKeyException();
            }
        }
    }

    /**
     * Kiểm key do client gửi lại: phải là key an toàn, bắt đầu bằng {@code {prefix}/{userId}/}, và phần còn lại là
     * đúng một đoạn {@code {uuid}.{ext}} mà module sinh ra.
     *
     * @throws InvalidObjectKeyException khi không đúng chủ hoặc không hợp lệ
     */
    public static void requireOwnedBy(String key, String prefix, UUID userId) {
        if (!isOwnedBy(key, prefix, userId)) {
            throw new InvalidObjectKeyException();
        }
    }

    /** Như {@link #requireOwnedBy} nhưng trả {@code false} và không ném. */
    public static boolean isOwnedBy(String key, String prefix, UUID userId) {
        if (prefix == null || userId == null) {
            return false;
        }
        try {
            requireSafe(key);
        } catch (InvalidObjectKeyException e) {
            return false;
        }
        String head = prefix + "/" + userId.toString().toLowerCase(Locale.ROOT) + "/";
        if (!key.startsWith(head)) {
            return false;
        }
        return GENERATED_FILE.matcher(key.substring(head.length())).matches();
    }

    private static void requirePrefix(String prefix) {
        if (prefix == null) {
            throw new IllegalArgumentException("object key prefix is required");
        }
        String[] segments = prefix.split("/", -1);
        if (segments.length > 3) {
            throw new IllegalArgumentException("object key prefix has too many segments");
        }
        for (String segment : segments) {
            if (!PREFIX_SEGMENT.matcher(segment).matches()) {
                throw new IllegalArgumentException("object key prefix is invalid");
            }
        }
    }

    private static void requireExtension(String extension) {
        if (extension == null || !EXTENSION.matcher(extension).matches()) {
            throw new IllegalArgumentException("object key extension is invalid");
        }
    }
}

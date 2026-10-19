package com.ieltspath.commonstorage.upload;

import java.util.Locale;
import java.util.Map;

/** Chuẩn hóa MIME khai báo và đuôi file tương ứng. */
public final class ContentTypes {

    private static final Map<String, String> ALIASES = Map.of(
            "audio/x-wav", "audio/wav",
            "audio/wave", "audio/wav",
            "audio/vnd.wave", "audio/wav",
            "audio/mp3", "audio/mpeg",
            "audio/x-m4a", "audio/mp4",
            "audio/m4a", "audio/mp4",
            "image/jpg", "image/jpeg");

    private static final Map<String, String> EXTENSIONS = Map.ofEntries(
            Map.entry("image/png", "png"),
            Map.entry("image/jpeg", "jpg"),
            Map.entry("image/webp", "webp"),
            Map.entry("image/gif", "gif"),
            Map.entry("audio/webm", "webm"),
            Map.entry("audio/mp4", "m4a"),
            Map.entry("audio/ogg", "ogg"),
            Map.entry("audio/mpeg", "mp3"),
            Map.entry("audio/wav", "wav"));

    private ContentTypes() {
    }

    /**
     * Bỏ tham số sau {@code ;} (ví dụ {@code audio/webm;codecs=opus} thành {@code audio/webm}), chuyển chữ thường,
     * đổi alias về MIME chuẩn. Trả {@code null} nếu rỗng.
     */
    public static String normalize(String declared) {
        if (declared == null) {
            return null;
        }
        int semicolon = declared.indexOf(';');
        String base = (semicolon >= 0 ? declared.substring(0, semicolon) : declared).trim().toLowerCase(Locale.ROOT);
        if (base.isEmpty()) {
            return null;
        }
        return ALIASES.getOrDefault(base, base);
    }

    /** Đuôi file cho MIME chuẩn. Loại không hỗ trợ ném {@link IllegalArgumentException}. */
    public static String extensionFor(String canonical) {
        String extension = EXTENSIONS.get(canonical);
        if (extension == null) {
            throw new IllegalArgumentException("content type has no extension mapping");
        }
        return extension;
    }
}

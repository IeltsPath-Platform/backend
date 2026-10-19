package com.ieltspath.commonstorage.upload;

import java.util.Optional;

/**
 * Nhận dạng loại file từ vài byte đầu (magic bytes).
 *
 * <p>Chặn được giả mạo rõ ràng: ảnh đội lốt audio, văn bản đội lốt ảnh, Matroska đội lốt webm. Không phải bộ giải mã
 * đầy đủ: file polyglot, MP4 có hình đội lốt {@code audio/mp4}, hay byte {@code FF Ex} ngẫu nhiên vẫn có thể qua.
 * Đừng coi đây là cổng bảo mật duy nhất.
 *
 * <p>Không nhận {@code image/svg+xml}: SVG là văn bản có thể chứa script.
 */
public final class ContentSniffer {

    private static final int MAX_WEBM_SCAN = 64;

    private ContentSniffer() {
    }

    /** MIME chuẩn của nội dung, hoặc rỗng nếu không nhận ra. Không ném, kể cả với mảng ngắn. */
    public static Optional<String> detect(byte[] content) {
        if (content == null || content.length == 0) {
            return Optional.empty();
        }
        if (startsWith(content, 0, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            return Optional.of("image/png");
        }
        if (startsWith(content, 0, 0xFF, 0xD8, 0xFF)) {
            return Optional.of("image/jpeg");
        }
        if (ascii(content, 0, "GIF87a") || ascii(content, 0, "GIF89a")) {
            return Optional.of("image/gif");
        }
        if (ascii(content, 0, "OggS")) {
            return Optional.of("audio/ogg");
        }
        if (ascii(content, 0, "RIFF") && ascii(content, 8, "WEBP")) {
            return Optional.of("image/webp");
        }
        if (ascii(content, 0, "RIFF") && ascii(content, 8, "WAVE")) {
            return Optional.of("audio/wav");
        }
        if (startsWith(content, 0, 0x1A, 0x45, 0xDF, 0xA3) && containsAscii(content, "webm", MAX_WEBM_SCAN)) {
            return Optional.of("audio/webm");
        }
        if (ascii(content, 4, "ftyp") && isMp4Brand(content)) {
            return Optional.of("audio/mp4");
        }
        if (ascii(content, 0, "ID3")) {
            return Optional.of("audio/mpeg");
        }
        if (content.length >= 2 && (content[0] & 0xFF) == 0xFF && ((content[1] & 0xE0) == 0xE0)) {
            return Optional.of("audio/mpeg");
        }
        return Optional.empty();
    }

    private static boolean isMp4Brand(byte[] content) {
        return ascii(content, 8, "M4A ") || ascii(content, 8, "mp42") || ascii(content, 8, "mp41")
                || ascii(content, 8, "isom") || ascii(content, 8, "iso2");
    }

    private static boolean startsWith(byte[] content, int offset, int... expected) {
        if (content.length < offset + expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if ((content[offset + i] & 0xFF) != expected[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean ascii(byte[] content, int offset, String text) {
        return startsWith(content, offset, toInts(text));
    }

    private static boolean containsAscii(byte[] content, String text, int window) {
        int limit = Math.min(content.length, window) - text.length();
        for (int i = 0; i <= limit; i++) {
            if (ascii(content, i, text)) {
                return true;
            }
        }
        return false;
    }

    private static int[] toInts(String text) {
        int[] values = new int[text.length()];
        for (int i = 0; i < text.length(); i++) {
            values[i] = text.charAt(i);
        }
        return values;
    }
}

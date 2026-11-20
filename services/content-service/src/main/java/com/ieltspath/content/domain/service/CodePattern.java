package com.ieltspath.content.domain.service;

import java.time.Year;
import java.text.Normalizer;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Validates and renders the small, user-configurable code pattern language. */
public final class CodePattern {
    private static final Pattern TOKEN = Pattern.compile("\\{([A-Z]+)(?::(\\d+))?}");
    private static final Set<String> ALL_TOKENS = Set.of("SEQ", "BAND", "COURSE", "TOPIC", "SKILL", "TYPE", "NAME", "YYYY");
    private static final Map<String, Set<String>> ALLOWED = Map.of(
            "COURSE", Set.of("SEQ", "BAND", "YYYY"),
            "TOPIC", Set.of("SEQ", "COURSE", "SKILL"),
            "LESSON", Set.of("SEQ", "TOPIC"),
            "PACKAGE", Set.of("SEQ", "COURSE", "TYPE"),
            "KNOWLEDGE_POINT", Set.of("NAME", "SKILL"),
            "ERROR_TYPE", Set.of("NAME"),
            "QUESTION", Set.of("SEQ", "YYYY"));

    public void validate(String kind, String pattern) {
        if (pattern == null || pattern.isBlank()) throw new IllegalArgumentException("Cấu trúc bộ mã không được để trống.");
        Matcher matcher = TOKEN.matcher(pattern);
        var tokens = new java.util.ArrayList<String>();
        while (matcher.find()) {
            String token = matcher.group(1);
            if (!ALL_TOKENS.contains(token)) throw new IllegalArgumentException("Từ khóa trong cấu trúc bộ mã không được hỗ trợ.");
            if (!ALLOWED.getOrDefault(kind, Set.of()).contains(token)) {
                throw new IllegalArgumentException("Từ khóa {" + token + "} không dùng được cho loại mã này.");
            }
            if (matcher.group(2) != null && !token.equals("SEQ")) {
                throw new IllegalArgumentException("Chỉ từ khóa {SEQ} mới hỗ trợ độ rộng số.");
            }
            if (matcher.group(2) != null) {
                try {
                    int width = Integer.parseInt(matcher.group(2));
                    if (width < 1 || width > 12) throw new IllegalArgumentException("Độ rộng của {SEQ} phải từ 1 đến 12.");
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException("Độ rộng của {SEQ} phải từ 1 đến 12.");
                }
            }
            tokens.add(token);
        }
        if (TOKEN.matcher(pattern).replaceAll("").matches(".*[{}].*")) {
            throw new IllegalArgumentException("Cấu trúc bộ mã có từ khóa không hợp lệ.");
        }
        if (kind.equals("COURSE") && !tokens.contains("SEQ") && !tokens.contains("BAND")) {
            throw new IllegalArgumentException("Cấu trúc mã khóa học cần có {SEQ} hoặc {BAND} để tránh trùng mã.");
        }
        if (!kind.equals("COURSE") && !tokens.contains("SEQ") && !tokens.contains("NAME")) {
            throw new IllegalArgumentException("Cấu trúc bộ mã cần có thành phần giúp tránh trùng mã.");
        }
    }

    public boolean usesSequence(String pattern) {
        if (pattern == null) return false;
        Matcher matcher = TOKEN.matcher(pattern);
        while (matcher.find()) if (matcher.group(1).equals("SEQ")) return true;
        return false;
    }

    public boolean hasToken(String pattern, String token) {
        if (pattern == null) return false;
        Matcher matcher = TOKEN.matcher(pattern);
        while (matcher.find()) if (matcher.group(1).equals(token)) return true;
        return false;
    }

    public boolean requiresContext(String pattern, Map<String, String> context) {
        Matcher matcher = TOKEN.matcher(pattern);
        while (matcher.find()) {
            String token = matcher.group(1);
            if (Set.of("BAND", "COURSE", "TOPIC", "SKILL", "TYPE", "NAME").contains(token)
                    && (context.get(token.toLowerCase(java.util.Locale.ROOT)) == null
                    || context.get(token.toLowerCase(java.util.Locale.ROOT)).isBlank())) return true;
        }
        return false;
    }

    public String render(String kind, String pattern, long sequence, Map<String, String> context) {
        validate(kind, pattern);
        Matcher matcher = TOKEN.matcher(pattern);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String token = matcher.group(1);
            String value = switch (token) {
                case "SEQ" -> sequence(sequence, matcher.group(2));
                case "YYYY" -> Integer.toString(Year.now().getValue());
                case "BAND" -> normalizeBand(context.get("band"));
                case "NAME" -> normalizeName(context.get("name"));
                default -> context.getOrDefault(token.toLowerCase(java.util.Locale.ROOT), "");
            };
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String sequence(long value, String widthText) {
        int width = widthText == null ? 1 : Integer.parseInt(widthText);
        return String.format(java.util.Locale.ROOT, "%0" + width + "d", value);
    }

    private static String normalizeBand(String band) {
        return band == null || band.isBlank() ? "" : new java.math.BigDecimal(band).setScale(1).toPlainString().replace('.', '_');
    }

    private static String normalizeName(String name) {
        if (name == null || name.isBlank()) return "";
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toUpperCase(java.util.Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_");
        return normalized.replaceAll("^_+|_+$", "");
    }
}

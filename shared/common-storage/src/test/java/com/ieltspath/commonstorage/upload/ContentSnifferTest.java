package com.ieltspath.commonstorage.upload;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ContentSnifferTest {

    private static byte[] bytes(int... values) {
        byte[] out = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = (byte) values[i];
        }
        return out;
    }

    private static byte[] concat(byte[] head, String ascii) {
        byte[] tail = ascii.getBytes(StandardCharsets.US_ASCII);
        byte[] out = new byte[head.length + tail.length];
        System.arraycopy(head, 0, out, 0, head.length);
        System.arraycopy(tail, 0, out, head.length, tail.length);
        return out;
    }

    private static byte[] riff(String form) {
        byte[] out = concat(new byte[]{'R', 'I', 'F', 'F', 0, 0, 0, 0}, form);
        return out;
    }

    private static byte[] mp4(String brand) {
        return concat(new byte[]{0, 0, 0, 0x18, 'f', 't', 'y', 'p'}, brand + "isom");
    }

    @Test
    void recognisesEachSupportedFormat() {
        assertThat(ContentSniffer.detect(bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0)))
                .contains("image/png");
        assertThat(ContentSniffer.detect(bytes(0xFF, 0xD8, 0xFF, 0xE0))).contains("image/jpeg");
        assertThat(ContentSniffer.detect(concat(new byte[0], "GIF87a"))).contains("image/gif");
        assertThat(ContentSniffer.detect(concat(new byte[0], "GIF89a"))).contains("image/gif");
        assertThat(ContentSniffer.detect(riff("WEBP"))).contains("image/webp");
        assertThat(ContentSniffer.detect(riff("WAVE"))).contains("audio/wav");
        assertThat(ContentSniffer.detect(bytes(0x1A, 0x45, 0xDF, 0xA3, 0x9F, 0x42, 0x82, 0x84)))
                .isEmpty();
        assertThat(ContentSniffer.detect(concat(bytes(0x1A, 0x45, 0xDF, 0xA3, 0x9F, 0x42, 0x82, 0x84), "webm")))
                .contains("audio/webm");
        assertThat(ContentSniffer.detect(mp4("M4A "))).contains("audio/mp4");
        assertThat(ContentSniffer.detect(mp4("mp42"))).contains("audio/mp4");
        assertThat(ContentSniffer.detect(concat(new byte[0], "OggS\0\0"))).contains("audio/ogg");
        assertThat(ContentSniffer.detect(concat(new byte[0], "ID3\u0003"))).contains("audio/mpeg");
        assertThat(ContentSniffer.detect(bytes(0xFF, 0xFB, 0x90, 0x00))).contains("audio/mpeg");
    }

    @Test
    void separatesRiffFormatsAndRejectsMatroskaAndUnknownBrands() {
        assertThat(ContentSniffer.detect(riff("WEBP"))).isNotEqualTo(ContentSniffer.detect(riff("WAVE")));
        assertThat(ContentSniffer.detect(mp4("qt  "))).isEmpty();
        assertThat(ContentSniffer.detect(concat(bytes(0x1A, 0x45, 0xDF, 0xA3), "matroska"))).isEmpty();
    }

    @Test
    void shortAndEmptyInputsDoNotThrow() {
        for (int length = 0; length <= 7; length++) {
            byte[] content = new byte[length];
            if (length > 0) {
                content[0] = (byte) 0xFF;
            }
            Optional<String> result = ContentSniffer.detect(content);
            assertThat(result).isNotNull();
        }
        assertThat(ContentSniffer.detect(new byte[0])).isEmpty();
        assertThat(ContentSniffer.detect(null)).isEmpty();
        assertThat(ContentSniffer.detect(bytes(0xFF))).isEmpty();
    }

    @Test
    void plainTextAndSvgAreNotRecognised() {
        assertThat(ContentSniffer.detect(concat(new byte[0], "<svg xmlns=\"http://www.w3.org/2000/svg\"/>"))).isEmpty();
        assertThat(ContentSniffer.detect(concat(new byte[0], "plain text not media"))).isEmpty();
        assertThat(ContentSniffer.detect(concat(new byte[0], "%PDF-1.4"))).isEmpty();
    }
}

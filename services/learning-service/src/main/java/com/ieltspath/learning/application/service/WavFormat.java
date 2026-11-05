package com.ieltspath.learning.application.service;

/**
 * Reads the header of a WAV file. The speech analysis takes only uncompressed 16-bit mono audio at 16 kHz, so the server
 * refuses anything else when it is stored instead of failing later, in the background, where the learner cannot fix it.
 */
public final class WavFormat {
    public static final int SAMPLE_RATE = 16_000;
    private static final int PCM = 1;

    private WavFormat() {
    }

    /** Whether the bytes are a WAV file of 16-bit PCM, one channel, 16 kHz, with some audio in it. */
    public static boolean isPcm16kMono(byte[] wav) {
        if (wav == null || wav.length < 44 || !ascii(wav, 0, "RIFF") || !ascii(wav, 8, "WAVE")) return false;
        boolean format = false;
        boolean audio = false;
        int position = 12;
        while (position + 8 <= wav.length) {
            long size = littleEndian(wav, position + 4, 4);
            int body = position + 8;
            if (ascii(wav, position, "fmt ")) {
                if (size < 16 || body + 16 > wav.length) return false;
                format = littleEndian(wav, body, 2) == PCM && littleEndian(wav, body + 2, 2) == 1
                        && littleEndian(wav, body + 4, 4) == SAMPLE_RATE && littleEndian(wav, body + 14, 2) == 16;
                if (!format) return false;
            } else if (ascii(wav, position, "data")) {
                audio = size > 0;
            }
            long next = body + size + (size % 2);
            if (next > wav.length) break;
            position = (int) next;
        }
        return format && audio;
    }

    /**
     * The audio samples of a WAV that {@link #isPcm16kMono} accepts, without the header. The speech service is fed the
     * samples through a stream, so no file with the learner's voice is written anywhere.
     *
     * @throws IllegalArgumentException when the bytes are not such a WAV
     */
    public static byte[] pcm(byte[] wav) {
        if (!isPcm16kMono(wav)) throw new IllegalArgumentException("not a 16 kHz mono 16-bit PCM WAV");
        int position = 12;
        while (position + 8 <= wav.length) {
            long size = littleEndian(wav, position + 4, 4);
            int body = position + 8;
            if (ascii(wav, position, "data")) {
                int end = (int) Math.min(wav.length, body + size);
                return java.util.Arrays.copyOfRange(wav, body, end - ((end - body) % 2));
            }
            long next = body + size + (size % 2);
            if (next > wav.length) break;
            position = (int) next;
        }
        throw new IllegalArgumentException("no audio in the WAV");
    }

    private static boolean ascii(byte[] bytes, int offset, String text) {
        if (offset + text.length() > bytes.length) return false;
        for (int index = 0; index < text.length(); index++) {
            if (bytes[offset + index] != (byte) text.charAt(index)) return false;
        }
        return true;
    }

    private static long littleEndian(byte[] bytes, int offset, int length) {
        long value = 0;
        for (int index = length - 1; index >= 0; index--) value = (value << 8) | (bytes[offset + index] & 0xFF);
        return value;
    }
}

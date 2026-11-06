package com.ieltspath.learning.application.service;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WavFormatTest {
    /** A canonical 44-byte header followed by {@code dataBytes} of silence. */
    static byte[] wav(int format, int channels, int sampleRate, int bits, int dataBytes) {
        ByteBuffer buffer = ByteBuffer.allocate(44 + dataBytes).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put("RIFF".getBytes()).putInt(36 + dataBytes).put("WAVE".getBytes());
        buffer.put("fmt ".getBytes()).putInt(16).putShort((short) format).putShort((short) channels)
                .putInt(sampleRate).putInt(sampleRate * channels * bits / 8).putShort((short) (channels * bits / 8))
                .putShort((short) bits);
        buffer.put("data".getBytes()).putInt(dataBytes);
        return buffer.array();
    }

    @Test
    void acceptsSixteenBitMonoPcmAtSixteenKilohertz() {
        assertTrue(WavFormat.isPcm16kMono(wav(1, 1, 16000, 16, 3200)));
    }

    @Test
    void refusesAnotherSampleRateChannelCountDepthOrEncoding() {
        assertFalse(WavFormat.isPcm16kMono(wav(1, 1, 44100, 16, 3200)));
        assertFalse(WavFormat.isPcm16kMono(wav(1, 2, 16000, 16, 3200)));
        assertFalse(WavFormat.isPcm16kMono(wav(1, 1, 16000, 8, 3200)));
        assertFalse(WavFormat.isPcm16kMono(wav(3, 1, 16000, 16, 3200)));
    }

    @Test
    void refusesAFileWithNoAudioInIt() {
        assertFalse(WavFormat.isPcm16kMono(wav(1, 1, 16000, 16, 0)));
    }

    @Test
    void refusesWhatIsNotWavOrIsTruncated() {
        assertFalse(WavFormat.isPcm16kMono(null));
        assertFalse(WavFormat.isPcm16kMono(new byte[10]));
        assertFalse(WavFormat.isPcm16kMono("RIFF....WAVEjunk".getBytes()));
        assertFalse(WavFormat.isPcm16kMono(java.util.Arrays.copyOf(wav(1, 1, 16000, 16, 100), 30)));
    }

    @Test
    void pcmIsTheSamplesWithoutTheHeader() {
        byte[] wav = wav(1, 1, 16000, 16, 400);
        for (int index = 44; index < wav.length; index++) wav[index] = (byte) (index % 251);

        byte[] pcm = WavFormat.pcm(wav);

        org.junit.jupiter.api.Assertions.assertEquals(400, pcm.length);
        org.junit.jupiter.api.Assertions.assertArrayEquals(java.util.Arrays.copyOfRange(wav, 44, 444), pcm);
    }

    @Test
    void pcmDropsAnExtraChunkAfterTheAudioAndRefusesAnUnsuitableFile() {
        byte[] plain = wav(1, 1, 16000, 16, 400);
        ByteBuffer buffer = ByteBuffer.allocate(plain.length + 12).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(plain).put("LIST".getBytes()).putInt(4).put("INFO".getBytes());

        org.junit.jupiter.api.Assertions.assertEquals(400, WavFormat.pcm(buffer.array()).length);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> WavFormat.pcm(wav(1, 2, 16000, 16, 400)));
    }

    @Test
    void findsTheFormatAfterAnExtraChunk() {
        byte[] plain = wav(1, 1, 16000, 16, 400);
        ByteBuffer buffer = ByteBuffer.allocate(plain.length + 12).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(plain, 0, 12).put("LIST".getBytes()).putInt(4).put("INFO".getBytes()).put(plain, 12, plain.length - 12);

        assertTrue(WavFormat.isPcm16kMono(buffer.array()));
    }
}

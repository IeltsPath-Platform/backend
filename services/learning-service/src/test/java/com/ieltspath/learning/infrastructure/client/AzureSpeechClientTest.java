package com.ieltspath.learning.infrastructure.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.application.exception.SpeechUnavailableException;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The parts of the client that need no network: configuration and the checks made before anything is sent. */
class AzureSpeechClientTest {
    private static AzureSpeechClient client(String key, String region) {
        return new AzureSpeechClient(new AzureSpeechSettings(key, region, "en-US", 30, 2), new ObjectMapper());
    }

    private static byte[] wav(int sampleRate) {
        int data = 3200;
        ByteBuffer buffer = ByteBuffer.allocate(44 + data).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put("RIFF".getBytes()).putInt(36 + data).put("WAVE".getBytes());
        buffer.put("fmt ".getBytes()).putInt(16).putShort((short) 1).putShort((short) 1).putInt(sampleRate)
                .putInt(sampleRate * 2).putShort((short) 2).putShort((short) 16);
        buffer.put("data".getBytes()).putInt(data);
        return buffer.array();
    }

    @Test
    void isAvailableOnlyWithBothAKeyAndARegion() {
        assertTrue(client("a-key", "eastasia").available());
        assertFalse(client("", "eastasia").available());
        assertFalse(client("a-key", "").available());
        assertFalse(client("", "").available());
    }

    @Test
    void refusesToWorkWhenNotConfiguredAndSaysItWillNotPass() {
        SpeechUnavailableException failure = assertThrows(SpeechUnavailableException.class,
                () -> client("", "").analyze(wav(16000), "en-US"));

        assertEquals("SPEECH_NOT_CONFIGURED", failure.getMessage());
        assertFalse(failure.isRetryable());
    }

    @Test
    void refusesAnUnsuitableRecordingBeforeSendingAnything() {
        // The key is a made-up one: if the client tried the network, the failure would not be this one.
        SpeechUnavailableException failure = assertThrows(SpeechUnavailableException.class,
                () -> client("a-key", "eastasia").analyze(wav(44100), "en-US"));

        assertEquals("RECORDING_FORMAT_UNSUPPORTED", failure.getMessage());
        assertFalse(failure.isRetryable());
    }

    @Test
    void settingsReportWhetherTheyAreConfigured() {
        assertTrue(new AzureSpeechSettings("k", "r", "en-GB", 30, 1).configured());
        assertFalse(new AzureSpeechSettings("", "", "en-GB", 30, 1).configured());
    }
}

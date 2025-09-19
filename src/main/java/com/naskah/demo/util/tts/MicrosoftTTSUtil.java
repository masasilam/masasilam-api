// MicrosoftTTSUtil.java
package com.naskah.demo.util.tts;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

@Slf4j
@Component
public class MicrosoftTTSUtil {

    /**
     * Generate speech using Microsoft Edge TTS (free alternative)
     * This is a simplified implementation - in production you'd use proper TTS service
     */
    public byte[] generateSpeech(String text, String voice, String speed, String pitch) {
        try {
            // For now, generate a simple audio placeholder
            // In real implementation, you would call Microsoft Cognitive Services TTS
            return generateDummyAudio(text);

        } catch (Exception e) {
            log.error("Error generating TTS", e);
            throw new RuntimeException("TTS generation failed", e);
        }
    }

    /**
     * Generate dummy audio for demonstration (replace with real TTS)
     */
    private byte[] generateDummyAudio(String text) {
        try {
            // Create a simple tone as placeholder
            int sampleRate = 44100;
            int duration = Math.min(text.length() / 10, 30); // Max 30 seconds
            int numSamples = sampleRate * duration;

            byte[] audioData = new byte[numSamples * 2]; // 16-bit audio

            // Generate simple tone
            for (int i = 0; i < numSamples; i++) {
                double angle = 2.0 * Math.PI * i * 440 / sampleRate; // 440Hz tone
                short sample = (short) (Math.sin(angle) * 16383); // 50% volume
                audioData[i * 2] = (byte) (sample & 0xFF);
                audioData[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
            }

            return audioData;

        } catch (Exception e) {
            log.error("Error generating dummy audio", e);
            return new byte[0];
        }
    }
}
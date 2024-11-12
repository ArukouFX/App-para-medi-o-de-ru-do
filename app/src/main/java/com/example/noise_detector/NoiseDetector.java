// NoiseDetector.java
package com.example.noise_detector;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

public class NoiseDetector {
    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private final NoiseLevelCallback callback;

    public interface NoiseLevelCallback {
        void onNoiseLevelUpdate(double decibels);
    }

    public NoiseDetector(NoiseLevelCallback callback) {
        this.callback = callback;
    }

    public void startMeasuringNoise(Context context) {
        int bufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        audioRecord.startRecording();
        isRecording = true;

        new Thread(() -> {
            byte[] buffer = new byte[bufferSize];
            while (isRecording) {
                int read = audioRecord.read(buffer, 0, bufferSize);
                if (read > 0) {
                    double decibels = calculateDecibels(buffer, read);
                    callback.onNoiseLevelUpdate(decibels);
                }
            }
        }).start();
    }

    public void stopMeasuringNoise() {
        isRecording = false;
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
        }
    }

    private double calculateDecibels(byte[] buffer, int length) {
        double rms = 0;
        for (int i = 0; i < length; i++) {
            rms += buffer[i] * buffer[i];
        }
        rms = Math.sqrt(rms / length);
        final double REFERENCE_PRESSURE = 20.0;
        return 20 * Math.log10(rms / REFERENCE_PRESSURE);
    }
}

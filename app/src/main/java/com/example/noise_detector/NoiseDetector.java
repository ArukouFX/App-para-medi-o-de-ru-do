package com.example.noise_detector;

import android.Manifest;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;

public class NoiseDetector {
    private static final String TAG = "NoiseDetector";
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord audioRecord;
    private boolean isRecording = false;

    public void startMeasuringNoise(Context context) {
        // Solicitar permiso de grabación de audio
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.checkSelfPermission(Manifest.permission.RECORD_AUDIO);
        }

        // Configurar AudioRecord
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize);

        // Iniciar la grabación
        audioRecord.startRecording();
        isRecording = true;

        // Procesar los datos de audio en un hilo aparte
        new Thread(() -> {
            byte[] buffer = new byte[bufferSize];
            while (isRecording) {
                int read = audioRecord.read(buffer, 0, bufferSize);
                if (read > 0) {
                    double decibels = calculateDecibels(buffer, read);
                    logNoiseLevel(decibels);
                }
            }
        }).start();
    }

    public void stopMeasuringNoise() {
        isRecording = false;
        audioRecord.stop();
        audioRecord.release();
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

    private void logNoiseLevel(double decibels) {
        String noiseLevel;
        if (decibels <= 60) {
            noiseLevel = "Seguro";
        } else if (decibels <= 85) {
            noiseLevel = "Moderado";
        } else if (decibels <= 100) {
            noiseLevel = "Peligroso";
        } else {
            noiseLevel = "Muy peligroso";
        }
        Log.d(TAG, "Nivel de ruido: " + decibels + " dB (" + noiseLevel + ")");
    }
}
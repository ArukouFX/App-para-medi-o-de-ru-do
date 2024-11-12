package com.example.noise_detector;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private NoiseDetector noiseDetector;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private boolean isRecording = false; // Para verificar o estado de gravação

    // UI Elements
    private TextView dbLevelText;
    private ProgressBar noiseLevelBar;
    private ProgressBar circularMeter;
    private TextView statusText;
    private TextView recordButtonText;
    private ImageButton recordButton;
    private ImageButton resetButton;  // Referência ao botão de reset
    private Handler uiHandler;
    private TextView minText;
    private TextView avgText;
    private TextView maxText;
    private double minDecibel = Double.MAX_VALUE;
    private double maxDecibel = Double.MIN_VALUE;
    private double totalDecibel = 0;
    private int readingCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar elementos de interface
        dbLevelText = findViewById(R.id.dbLevelText);
        noiseLevelBar = findViewById(R.id.noiseLevelBar);
        circularMeter = findViewById(R.id.circularMeter);
        statusText = findViewById(R.id.statusText);
        recordButton = findViewById(R.id.recordButton);
        resetButton = findViewById(R.id.resetButton);  // Inicializa o botão de reset
        recordButtonText = findViewById(R.id.recordButtonText);
        uiHandler = new Handler(Looper.getMainLooper());
        minText = findViewById(R.id.minText);
        avgText = findViewById(R.id.avgText);
        maxText = findViewById(R.id.maxText);

        // Solicitar permissão de áudio
        requestAudioPermission();

        // Configura o listener do botão de reset dentro do onCreate
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Chama o método de reset
                resetValues(view);
            }
        });
    }

    private void requestAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
        } else {
            recordButton.setEnabled(true);
        }
    }

    private void stopMeasuringNoise() {
        if (noiseDetector != null) {
            noiseDetector.stopMeasuringNoise();
        }

        // Atualiza o estado da gravação
        isRecording = false;

        // Atualiza o ícone e texto do botão
        recordButton.setImageResource(R.drawable.ic_microphone_start);  // Ícone para iniciar
        recordButtonText.setText("Iniciar gravação");  // Texto de "Iniciar gravação"

        // Habilita o botão de reset e altera a cor para o estado normal
        resetButton.setEnabled(true);
        resetButton.setBackgroundResource(R.drawable.button_background);  // Cor normal para o botão
    }

    public void toggleRecording(View view) {
        if (isRecording) {
            stopMeasuringNoise();
        } else {
            startMeasuringNoise();
        }
    }

    private void startMeasuringNoise() {
        noiseDetector = new NoiseDetector(decibels -> uiHandler.post(() -> updateNoiseLevelUI(decibels)));
        noiseDetector.startMeasuringNoise(this);

        // Atualiza o estado da gravação
        isRecording = true;

        // Atualiza o ícone e texto do botão
        recordButton.setImageResource(R.drawable.ic_microphone_stop);  // Ícone para parar
        recordButtonText.setText("Parar gravação");  // Texto de "Parar gravação"

        // Desabilita o botão de reset e altera a cor para o estado desabilitado
        resetButton.setEnabled(false);
        resetButton.setBackgroundResource(R.drawable.button_background_disabled);  // Cor para desabilitado
    }

    private void updateNoiseLevelUI(double decibels) {
        // Atualizar a interface com o nível de decibéis
        dbLevelText.setText(String.format("%.2f dB", decibels));
        noiseLevelBar.setProgress((int) decibels);
        circularMeter.setProgress((int) decibels);

        // Atualizar o mínimo, máximo e média
        if (decibels < minDecibel) {
            minDecibel = decibels;
        }
        if (decibels > maxDecibel) {
            maxDecibel = decibels;
        }
        totalDecibel += decibels;
        readingCount++;
        double avgDecibel = totalDecibel / readingCount;

        // Atualizar textos de mínimo, média e máximo
        minText.setText(String.format("%.2f dB", minDecibel));
        avgText.setText(String.format("%.2f dB", avgDecibel));
        maxText.setText(String.format("%.2f dB", maxDecibel));

        // Definir o status com base nos decibéis
        String status = "Seguro";  // Lógica de status (por exemplo)
        if (decibels > 85) {
            status = "Peligroso";
        }
        statusText.setText("Status: " + status);
    }

    public void resetValues(View view) {
        Log.d("Reset", "Botão de reset pressionado");

        if (isRecording) {
            // Se a gravação estiver em andamento, apenas ignora a ação (não exibe mais o Toast)
            Log.d("Reset", "Gravação em andamento. Não é possível resetar.");
        } else {
            // Se a gravação estiver parada, zera os valores
            Log.d("Reset", "Gravação parada. Resetando valores.");
            minText.setText("0 dB");
            avgText.setText("0 dB");
            maxText.setText("0 dB");

            // Zera as barras de progresso
            noiseLevelBar.setProgress(0);
            circularMeter.setProgress(0);

            // Zera os valores internos para o cálculo dos decibéis
            minDecibel = Double.MAX_VALUE;
            maxDecibel = Double.MIN_VALUE;
            totalDecibel = 0;
            readingCount = 0;

            // Atualiza o texto do nível de decibéis
            dbLevelText.setText("0 dB");

            // Exibe o Toast para confirmar o reset
            Log.d("Reset", "Valores resetados. Exibindo Toast.");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "Valores resetados", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Parar a medição de ruído ao destruir a atividade
        if (noiseDetector != null) {
            noiseDetector.stopMeasuringNoise();
        }
    }
}

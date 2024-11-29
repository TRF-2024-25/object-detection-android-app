package com.surendramaran.yolov8tflite;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    private static final int DETECTION_REQUEST_CODE = 1;
    private TextView resultTextView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Button startDetectionButton = findViewById(R.id.startDetectionButton);
        resultTextView = findViewById(R.id.resultTextView);

        // Set button to start MainActivity
        startDetectionButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, MainActivity.class);
            startActivityForResult(intent, DETECTION_REQUEST_CODE);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == DETECTION_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            String detectedObjects = data.getStringExtra("detectedObjects");
            resultTextView.setText(detectedObjects);
        }
    }
}

package com.example.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle saved_instance_state) {
        super.onCreate(saved_instance_state);
        setContentView(R.layout.activity_splash);

        TextView time_view = findViewById(R.id.splash_time);
        Button start_btn = findViewById(R.id.start_btn);

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        time_view.setText(sdf.format(new Date()));

        start_btn.setOnClickListener(v -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }
}

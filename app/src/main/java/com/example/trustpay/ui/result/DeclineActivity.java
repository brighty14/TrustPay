package com.example.trustpay.ui.result;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.trustpay.R;
import com.example.trustpay.ui.dashboard.DashboardActivity;

import org.json.JSONObject;

public class DeclineActivity extends AppCompatActivity {

    Button btnBackDashboard;
    TextView tvReason;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decline);

        btnBackDashboard = findViewById(R.id.btnBackDashboard);
        tvReason = findViewById(R.id.tvReason);

        // Receive failure reason
        String reason = getIntent().getStringExtra("reason");

        if (reason != null) {
            tvReason.setText(formatReason(reason));
        }

        btnBackDashboard.setOnClickListener(v -> {

            Intent intent = new Intent(DeclineActivity.this, DashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            startActivity(intent);
            finish();
        });
    }

    private String formatReason(String reason) {
        String cleanReason = reason.trim();

        if (cleanReason.startsWith("{") && cleanReason.endsWith("}")) {
            try {
                JSONObject jsonObject = new JSONObject(cleanReason);
                String message = jsonObject.optString("message", "");
                if (!message.isEmpty()) {
                    return message;
                }
            } catch (Exception ignored) {
            }
        }

        return cleanReason;
    }
}

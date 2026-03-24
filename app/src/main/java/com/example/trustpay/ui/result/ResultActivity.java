package com.example.trustpay.ui.result;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.trustpay.R;
import com.example.trustpay.ui.dashboard.DashboardActivity;
import com.google.android.material.button.MaterialButton;

public class ResultActivity extends AppCompatActivity {

    MaterialButton btnBack;
    String upi; // ✅ declare only

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        // ✅ NOW get intent here
        upi = getIntent().getStringExtra("upi");

        btnBack = findViewById(R.id.btnBackDashboard);

        btnBack.setOnClickListener(v -> {

            Intent intent = new Intent(ResultActivity.this, DashboardActivity.class);

            intent.putExtra("upi", upi);

            startActivity(intent);
            finish();
        });
    }
}
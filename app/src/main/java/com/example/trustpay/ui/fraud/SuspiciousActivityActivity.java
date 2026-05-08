package com.example.trustpay.ui.fraud;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.trustpay.R;
import com.example.trustpay.ui.liveness.LivenessActivity;

public class SuspiciousActivityActivity extends AppCompatActivity {

    Button btnVerify, btnCancel;

    String senderUpi;
    String receiverUpi;
    String amount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suspicious);

        btnVerify = findViewById(R.id.btnVerify);
        btnCancel = findViewById(R.id.btnCancel);

        // ✅ Get data INSIDE onCreate
        senderUpi = getIntent().getStringExtra("sender_upi");
        receiverUpi = getIntent().getStringExtra("receiver_upi");
        amount = getIntent().getStringExtra("amount");

        btnVerify.setOnClickListener(v -> {

            Intent intent = new Intent(this, LivenessActivity.class);

            // ✅ Pass data forward
            intent.putExtra("sender_upi", senderUpi);
            intent.putExtra("receiver_upi", receiverUpi);
            intent.putExtra("amount", amount);

            

            startActivity(intent);
        });

        btnCancel.setOnClickListener(v -> finish());
    }
}

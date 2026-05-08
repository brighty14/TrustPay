package com.example.trustpay.ui.dashboard;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.trustpay.R;
import com.example.trustpay.ui.auth.LoginActivity;
import com.example.trustpay.ui.profile.ProfileActivity;
import com.example.trustpay.ui.transaction.QrScannerActivity;
import com.example.trustpay.ui.transaction.TransactionActivity;
import com.example.trustpay.ui.history.HistoryActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class DashboardActivity extends AppCompatActivity {

    MaterialCardView cardNewTransaction, cardHistory;
    MaterialButton btnScanPay;
    ImageView profileIcon;

    String name, email, mobile, upi, senderUpi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);


        cardNewTransaction = findViewById(R.id.cardNewTransaction);
        cardHistory = findViewById(R.id.cardHistory);
        btnScanPay = findViewById(R.id.btnScanPay);
        profileIcon = findViewById(R.id.profileIcon);

        // Receive user details from LoginActivity
        SharedPreferences prefs = getSharedPreferences("user_data", MODE_PRIVATE);

        if (!prefs.getBoolean("is_logged_in", false)) {
            Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }

        name = prefs.getString("name", "");
        email = prefs.getString("email", "");
        mobile = prefs.getString("mobile", "");
        upi = prefs.getString("upi", "");

        Toast.makeText(this, "UPI: " + upi, Toast.LENGTH_LONG).show();

        Toast.makeText(this, "UPI: " + upi, Toast.LENGTH_LONG).show();

        // Open Transaction Screen
        cardNewTransaction.setOnClickListener(v -> {

            Intent intent = new Intent(DashboardActivity.this, TransactionActivity.class);

            intent.putExtra("upi", upi); // ✅ FIXED

            startActivity(intent);
        });

        // Open History Screen
        cardHistory.setOnClickListener(v -> {

            if (upi == null || upi.isEmpty()) {
                Toast.makeText(this, "UPI missing in Dashboard", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(DashboardActivity.this, HistoryActivity.class);
            startActivity(intent);
        });

        // Open Profile Screen
        profileIcon.setOnClickListener(v -> {

            Intent intent = new Intent(DashboardActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        btnScanPay.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, QrScannerActivity.class);
            intent.putExtra("sender_upi", upi);
            startActivity(intent);
        });
    }
}

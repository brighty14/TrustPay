package com.example.trustpay.ui.dashboard;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.trustpay.R;
import com.example.trustpay.ui.profile.ProfileActivity;
import com.example.trustpay.ui.transaction.TransactionActivity;
import com.example.trustpay.ui.history.HistoryActivity;
import com.google.android.material.card.MaterialCardView;

public class DashboardActivity extends AppCompatActivity {

    MaterialCardView cardNewTransaction, cardHistory;
    ImageView profileIcon;

    String name, email, mobile, upi, senderUpi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);


        cardNewTransaction = findViewById(R.id.cardNewTransaction);
        cardHistory = findViewById(R.id.cardHistory);
        profileIcon = findViewById(R.id.profileIcon);

        // Receive user details from LoginActivity
        SharedPreferences prefs = getSharedPreferences("user_data", MODE_PRIVATE);

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
    }
}
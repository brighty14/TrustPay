package com.example.trustpay.ui.profile;

import android.os.Bundle;
import android.content.SharedPreferences;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.trustpay.R;

public class ProfileActivity extends AppCompatActivity {

    TextView tvName, tvEmail, tvMobile, tvUpi, tvBalance;

    String BASE_URL = "http://10.41.17.76:5000/balance/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        tvName = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        tvMobile = findViewById(R.id.tvMobile);
        tvUpi = findViewById(R.id.tvUpi);
        tvBalance = findViewById(R.id.tvBalance); // ✅ NEW

        SharedPreferences prefs = getSharedPreferences("user_data", MODE_PRIVATE);

        String name = prefs.getString("name", "");
        String email = prefs.getString("email", "");
        String mobile = prefs.getString("mobile", "");
        String upi = prefs.getString("upi", "");

        tvName.setText("Name: " + name);
        tvEmail.setText("Email: " + email);
        tvMobile.setText("Mobile: " + mobile);
        tvUpi.setText("UPI ID: " + upi);

        // 🔥 Fetch balance from backend
        fetchBalance(upi);
    }

    // -------------------------------
    // Fetch Balance API
    // -------------------------------
    private void fetchBalance(String upi) {

        String url = BASE_URL + upi;

        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {

                    String balance = response.optString("balance");
                    tvBalance.setText("Balance: ₹" + balance);

                },
                error -> {
                    Toast.makeText(this, "Failed to fetch balance", Toast.LENGTH_SHORT).show();
                }
        );

        queue.add(request);
    }
}

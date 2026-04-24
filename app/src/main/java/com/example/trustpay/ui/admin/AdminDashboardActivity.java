package com.example.trustpay.ui.admin;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.trustpay.R;
import com.example.trustpay.network.BackendConfig;
import com.example.trustpay.ui.auth.LoginActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AdminDashboardActivity extends AppCompatActivity {

    public int totalTxnsToday;
    public int anomalyCount;
    public double fraudRate;
    public String alertMessage;
    public int normalCount;
    public List<AdminTransaction> transactionList = new ArrayList<>();

    String DASHBOARD_DATA_URL = BackendConfig.endpoint("admin/dashboard-data");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        findViewById(R.id.btnAdminLogout).setOnClickListener(v -> logout());

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(navListener);

        fetchDashboardData();
    }

    private BottomNavigationView.OnNavigationItemSelectedListener navListener =
            item -> {
                Fragment selectedFragment = null;

                int itemId = item.getItemId();
                if (itemId == R.id.nav_insights) {
                    selectedFragment = new AdminInsightsFragment();
                } else if (itemId == R.id.nav_charts) {
                    selectedFragment = new AdminChartsFragment();
                } else if (itemId == R.id.nav_transactions) {
                    selectedFragment = new AdminTransactionsFragment();
                }

                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, selectedFragment)
                            .commit();
                }
                return true;
            };

    private void fetchDashboardData() {
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                DASHBOARD_DATA_URL,
                null,
                response -> {
                    try {
                        normalCount = response.optInt("normal_count", 0);
                        anomalyCount = response.optInt("anomaly_count", 0);
                        totalTxnsToday = response.optInt("total_txns_today", 0);
                        fraudRate = response.optDouble("fraud_rate", 0.0);

                        JSONArray alertsArray = response.optJSONArray("alerts");
                        if (alertsArray != null && alertsArray.length() > 0) {
                            alertMessage = alertsArray.optString(0);
                        }

                        transactionList.clear();
                        JSONArray txArray = response.optJSONArray("transactions");
                        if (txArray != null) {
                            for (int i = 0; i < txArray.length(); i++) {
                                JSONObject obj = txArray.getJSONObject(i);
                                String sender = obj.optString("sender_upi");
                                String receiver = obj.optString("receiver_upi");
                                double amount = obj.optDouble("amount");
                                String status = obj.optString("status");
                                String timestamp = obj.optString("timestamp");
                                boolean isAnomaly = obj.optBoolean("is_anomaly", false);
                                int riskScore = obj.optInt("risk_score", 0);
                                String riskLevel = obj.optString("risk_level", "Low");

                                transactionList.add(new AdminTransaction(
                                        sender, receiver, amount, status, timestamp, isAnomaly, riskScore, riskLevel
                                ));
                            }
                        }

                        // Load initial fragment after data is ready
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, new AdminInsightsFragment())
                                .commit();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(this, "Failed to load dashboard data", Toast.LENGTH_SHORT).show()
        );

        queue.add(request);
    }

    private void logout() {
        SharedPreferences prefs = getSharedPreferences("user_data", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

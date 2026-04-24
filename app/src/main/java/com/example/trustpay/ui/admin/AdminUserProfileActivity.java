package com.example.trustpay.ui.admin;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.trustpay.R;
import com.example.trustpay.network.BackendConfig;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AdminUserProfileActivity extends AppCompatActivity {

    TextView tvProfileName, tvProfileUpi, tvProfileMobile;
    TextView tvProfileRiskScore, tvProfileTotalTxns, tvProfileAnomalies;
    MaterialCardView cardRiskHeader;
    RecyclerView recyclerView;
    AdminTransactionAdapter adapter;
    List<AdminTransaction> transactionList;

    String upiId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_user_profile);

        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileUpi = findViewById(R.id.tvProfileUpi);
        tvProfileMobile = findViewById(R.id.tvProfileMobile);
        tvProfileRiskScore = findViewById(R.id.tvProfileRiskScore);
        tvProfileTotalTxns = findViewById(R.id.tvProfileTotalTxns);
        tvProfileAnomalies = findViewById(R.id.tvProfileAnomalies);
        cardRiskHeader = findViewById(R.id.cardRiskHeader);
        recyclerView = findViewById(R.id.recyclerProfileTransactions);

        transactionList = new ArrayList<>();
        adapter = new AdminTransactionAdapter(transactionList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        upiId = getIntent().getStringExtra("upi_id");
        if (upiId != null) {
            fetchUserProfile();
        } else {
            Toast.makeText(this, "User ID not provided", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void fetchUserProfile() {
        String url = BackendConfig.endpoint("admin/user-profile/" + upiId);
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        String name = response.optString("name");
                        String mobile = response.optString("mobile");
                        int totalTxns = response.optInt("total_txns", 0);
                        int anomalies = response.optInt("anomalies", 0);
                        int riskScore = response.optInt("risk_score", 0);
                        String riskLevel = response.optString("risk_level", "Low");

                        tvProfileName.setText("Name: " + name);
                        tvProfileUpi.setText("UPI: " + upiId);
                        tvProfileMobile.setText("Mobile: " + mobile);

                        tvProfileTotalTxns.setText(String.valueOf(totalTxns));
                        tvProfileAnomalies.setText(String.valueOf(anomalies));
                        tvProfileRiskScore.setText(riskScore + "/100");

                        if ("High".equalsIgnoreCase(riskLevel)) {
                            tvProfileRiskScore.setTextColor(Color.parseColor("#D32F2F"));
                            cardRiskHeader.setCardBackgroundColor(Color.parseColor("#FFEBEE"));
                        } else if ("Medium".equalsIgnoreCase(riskLevel)) {
                            tvProfileRiskScore.setTextColor(Color.parseColor("#F57C00"));
                            cardRiskHeader.setCardBackgroundColor(Color.parseColor("#FFF3E0"));
                        } else {
                            tvProfileRiskScore.setTextColor(Color.parseColor("#2E7D32"));
                            cardRiskHeader.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
                        }

                        transactionList.clear();
                        JSONArray txArray = response.optJSONArray("recent_txns");
                        if (txArray != null) {
                            for (int i = 0; i < txArray.length(); i++) {
                                JSONObject obj = txArray.getJSONObject(i);
                                String receiver = obj.optString("receiver_upi");
                                double amount = obj.optDouble("amount");
                                String status = obj.optString("status");
                                String timestamp = obj.optString("timestamp");
                                boolean isAnomaly = obj.optBoolean("is_anomaly", false);
                                String txRiskLevel = obj.optString("risk_level", "Low");

                                transactionList.add(new AdminTransaction(
                                        upiId, receiver, amount, status, timestamp, isAnomaly, riskScore, txRiskLevel
                                ));
                            }
                        }
                        adapter.notifyDataSetChanged();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(this, "Failed to load user profile", Toast.LENGTH_SHORT).show()
        );

        queue.add(request);
    }
}

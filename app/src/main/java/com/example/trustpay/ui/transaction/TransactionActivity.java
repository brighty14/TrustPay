package com.example.trustpay.ui.transaction;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.trustpay.R;
import com.example.trustpay.network.BackendConfig;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class TransactionActivity extends AppCompatActivity {

    TextInputEditText editTextReceiverUpi;
    MaterialCardView cardReceiverResult;
    TextView tvReceiverName, tvReceiverDetails, tvLookupStatus;

    private final Handler lookupHandler = new Handler(Looper.getMainLooper());
    private RequestQueue queue;
    private String senderUpi;
    private String receiverName;
    private String receiverMobile;
    private String receiverUpi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        editTextReceiverUpi = findViewById(R.id.editTextReceiverUpi);
        cardReceiverResult = findViewById(R.id.cardReceiverResult);
        tvReceiverName = findViewById(R.id.tvReceiverName);
        tvReceiverDetails = findViewById(R.id.tvReceiverDetails);
        tvLookupStatus = findViewById(R.id.tvLookupStatus);

        queue = Volley.newRequestQueue(this);
        senderUpi = getIntent().getStringExtra("upi");

        cardReceiverResult.setVisibility(View.GONE);
        tvLookupStatus.setText("");

        editTextReceiverUpi.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                lookupHandler.removeCallbacksAndMessages(null);
                cardReceiverResult.setVisibility(View.GONE);

                String input = s.toString().trim();
                if (input.length() < 4) {
                    tvLookupStatus.setText("");
                    return;
                }

                tvLookupStatus.setText("Finding receiver...");
                lookupHandler.postDelayed(() -> lookupReceiver(input), 500);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });

        cardReceiverResult.setOnClickListener(v -> openPaymentPage());
    }

    private void lookupReceiver(String input) {
        try {
            String encodedInput = URLEncoder.encode(input, StandardCharsets.UTF_8.name());
            String url = BackendConfig.endpoint("receiver/" + encodedInput);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.GET,
                    url,
                    null,
                    response -> showReceiver(response),
                    error -> {
                        receiverUpi = null;
                        cardReceiverResult.setVisibility(View.GONE);
                        tvLookupStatus.setText("Receiver not found");
                    }
            );

            queue.add(request);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to search receiver", Toast.LENGTH_SHORT).show();
        }
    }

    private void showReceiver(JSONObject response) {
        if (!response.optBoolean("success", false)) {
            tvLookupStatus.setText("Receiver not found");
            cardReceiverResult.setVisibility(View.GONE);
            return;
        }

        receiverName = response.optString("name");
        receiverMobile = response.optString("mobile");
        receiverUpi = response.optString("upi");

        tvReceiverName.setText(receiverName);
        tvReceiverDetails.setText(receiverMobile + " | " + receiverUpi);
        tvLookupStatus.setText("Tap receiver to pay");
        cardReceiverResult.setVisibility(View.VISIBLE);
    }

    private void openPaymentPage() {
        if (receiverUpi == null || receiverUpi.isEmpty()) {
            Toast.makeText(this, "Select a valid receiver", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(TransactionActivity.this, PaymentActivity.class);
        intent.putExtra("sender_upi", senderUpi);
        intent.putExtra("receiver_name", receiverName);
        intent.putExtra("receiver_mobile", receiverMobile);
        intent.putExtra("receiver_upi", receiverUpi);
        startActivity(intent);
    }
}

package com.example.trustpay.ui.transaction;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.example.trustpay.R;
import com.example.trustpay.network.BackendConfig;
import com.example.trustpay.ui.fraud.SuspiciousActivityActivity;
import com.example.trustpay.ui.verification.PinActivity;
import com.example.trustpay.utils.FraudDetector;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PaymentActivity extends AppCompatActivity {

    TextView tvReceiverName, tvReceiverMobile, tvReceiverUpi;
    TextInputEditText editTextAmount;
    MaterialButton btnPay;

    private String senderUpi;
    private String receiverName;
    private String receiverMobile;
    private String receiverUpi;

    String HISTORY_URL = BackendConfig.endpoint("history");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        tvReceiverName = findViewById(R.id.tvReceiverName);
        tvReceiverMobile = findViewById(R.id.tvReceiverMobile);
        tvReceiverUpi = findViewById(R.id.tvReceiverUpi);
        editTextAmount = findViewById(R.id.editTextAmount);
        btnPay = findViewById(R.id.btnPay);

        senderUpi = getIntent().getStringExtra("sender_upi");
        receiverName = getIntent().getStringExtra("receiver_name");
        receiverMobile = getIntent().getStringExtra("receiver_mobile");
        receiverUpi = getIntent().getStringExtra("receiver_upi");

        tvReceiverName.setText(receiverName == null ? "" : receiverName);
        tvReceiverMobile.setText("Phone: " + (receiverMobile == null ? "" : receiverMobile));
        tvReceiverUpi.setText("UPI ID: " + (receiverUpi == null ? "" : receiverUpi));

        btnPay.setOnClickListener(v -> startPaymentCheck());
    }

    private void startPaymentCheck() {
        String amountStr = editTextAmount.getText() == null
                ? ""
                : editTextAmount.getText().toString().trim();

        if (amountStr.isEmpty()) {
            editTextAmount.setError("Enter amount");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            editTextAmount.setError("Enter valid amount");
            return;
        }

        if (amount <= 0) {
            editTextAmount.setError("Amount must be greater than 0");
            return;
        }

        if (receiverUpi == null || receiverUpi.isEmpty()) {
            Toast.makeText(this, "Receiver UPI missing", Toast.LENGTH_SHORT).show();
            return;
        }

        fetchTransactionHistory(amount);
    }

    private void fetchTransactionHistory(double amount) {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = HISTORY_URL + "?upi=" + senderUpi;

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        List<Double> history = new ArrayList<>();

                        for (int i = 0; i < response.length(); i++) {
                            JSONObject obj = response.getJSONObject(i);
                            double amt = obj.getDouble("amount");
                            String type = obj.getString("type");

                            if (type.equalsIgnoreCase("SENT")) {
                                history.add(amt);
                            }
                        }

                        if (history.isEmpty()) {
                            goToPin(amount);
                            return;
                        }

                        boolean isFraud = FraudDetector.isTransactionSuspicious(history, amount);

                        if (isFraud) {
                            Intent intent = new Intent(PaymentActivity.this, SuspiciousActivityActivity.class);
                            intent.putExtra("sender_upi", senderUpi);
                            intent.putExtra("receiver_upi", receiverUpi);
                            intent.putExtra("amount", String.valueOf(amount));
                            startActivity(intent);
                        } else {
                            goToPin(amount);
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "Error processing transaction", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Server error. Try again.", Toast.LENGTH_LONG).show()
        );

        queue.add(request);
    }

    private void goToPin(double amount) {
        Intent intent = new Intent(PaymentActivity.this, PinActivity.class);
        intent.putExtra("sender_upi", senderUpi);
        intent.putExtra("receiver_upi", receiverUpi);
        intent.putExtra("amount", String.valueOf(amount));
        startActivity(intent);
    }
}

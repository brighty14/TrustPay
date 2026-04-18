package com.example.trustpay.ui.transaction;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.example.trustpay.R;
import com.example.trustpay.ui.fraud.SuspiciousActivityActivity;
import com.example.trustpay.ui.verification.PinActivity;
import com.example.trustpay.utils.FraudDetector;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TransactionActivity extends AppCompatActivity {

    EditText editTextAmount;
    EditText editTextReceiverUpi;
    Button btnPay;
    String senderUpi;
    String receiverUpi;


    String HISTORY_URL = "http://10.41.17.76:5000/history";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        //Toast.makeText(this, "Sender UPI: " + senderUpi, Toast.LENGTH_LONG).show();
        editTextAmount = findViewById(R.id.editTextAmount);
        editTextReceiverUpi = findViewById(R.id.editTextReceiverUpi);
        btnPay = findViewById(R.id.btnPay);
        senderUpi = getIntent().getStringExtra("upi");
        if (senderUpi == null) {
            Toast.makeText(this, "UPI not received", Toast.LENGTH_LONG).show();
            senderUpi = "test@upi"; // fallback
        }
        btnPay.setOnClickListener(v -> {

            String amountStr = editTextAmount.getText().toString();

            if (amountStr.isEmpty()) {
                editTextAmount.setError("Enter amount");
                return;
            }

            double amount = Double.parseDouble(amountStr);

            // 🔥 Call backend
            fetchTransactionHistory(amount);
            receiverUpi = editTextReceiverUpi.getText().toString().trim();

            if (receiverUpi.isEmpty()) {
                editTextReceiverUpi.setError("Enter receiver UPI");
                return;
            }
        });


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

                        // 🔥 IMPORTANT: Handle empty history
                        if (history.isEmpty()) {
                            Toast.makeText(this, "No transaction history found", Toast.LENGTH_SHORT).show();

                            goToPin(amount);
                            return;
                        }

                        boolean isFraud = FraudDetector.isTransactionSuspicious(history, amount);

                        if (isFraud) {
                            Intent intent = new Intent(TransactionActivity.this, SuspiciousActivityActivity.class);
                            intent.putExtra("sender_upi", senderUpi);
                            intent.putExtra("receiver_upi", receiverUpi);
                            intent.putExtra("amount", String.valueOf(amount));
                            startActivity(intent);
                        } else {
                            goToPin(amount);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error processing data", Toast.LENGTH_SHORT).show();
                    }
                },

                error -> {
                    error.printStackTrace();

                    Toast.makeText(this, "Server error. Try again.", Toast.LENGTH_LONG).show();
                }
        );

        queue.add(request);
    }

    // 🔥 Clean reusable method
    private void goToPin(double amount) {

        Intent intent = new Intent(TransactionActivity.this, PinActivity.class);

        intent.putExtra("sender_upi", senderUpi);
        intent.putExtra("receiver_upi", receiverUpi);
        intent.putExtra("amount", String.valueOf(amount));

        startActivity(intent);
    }
}

package com.example.trustpay.ui.verification;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.trustpay.R;
import com.example.trustpay.ui.result.ResultActivity;
import com.example.trustpay.ui.result.DeclineActivity;

import org.json.JSONObject;

public class PinActivity extends AppCompatActivity {

    EditText etPin;
    Button btnConfirmPin;

    String senderUpi;
    String receiverUpi;
    String amount;

    ProgressDialog progressDialog;

    String BASE_URL = "http://10.41.17.76:5000/transaction";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin);

        etPin = findViewById(R.id.etPin);
        btnConfirmPin = findViewById(R.id.btnConfirmPin);

        senderUpi = getIntent().getStringExtra("sender_upi");
        receiverUpi = getIntent().getStringExtra("receiver_upi");
        amount = getIntent().getStringExtra("amount");

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Processing Payment...");
        progressDialog.setCancelable(false);

        btnConfirmPin.setOnClickListener(v -> verifyPinAndPay());
    }

    private void verifyPinAndPay() {

        String pin = etPin.getText().toString().trim();

        if (pin.length() != 4) {
            showErrorDialog("Enter valid 4 digit PIN");
            return;
        }

        progressDialog.show();

        try {

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("sender_upi", senderUpi);
            jsonBody.put("receiver_upi", receiverUpi);
            jsonBody.put("amount", amount);
            jsonBody.put("upi_pin", pin);

            RequestQueue queue = Volley.newRequestQueue(this);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    BASE_URL,
                    jsonBody,

                    response -> {

                        progressDialog.dismiss();
                        android.util.Log.d("API_RESPONSE", response.toString());

                        try {

                            boolean success = response.optBoolean("success", false);

                            if (success) {

                                Intent intent = new Intent(PinActivity.this, ResultActivity.class);
                                intent.putExtra("upi", senderUpi);
                                startActivity(intent);
                                finish();

                            } else {

                                Intent intent = new Intent(PinActivity.this, DeclineActivity.class);
                                intent.putExtra("reason", response.optString("message", "Transaction Failed"));

                                startActivity(intent);
                                finish();
                            }

                        } catch (Exception e) {
                            e.printStackTrace();

                            Intent intent = new Intent(PinActivity.this, DeclineActivity.class);
                            intent.putExtra("reason", "Unexpected error");

                            startActivity(intent);
                            finish();
                        }
                    },

                    error -> {

                        progressDialog.dismiss();

                        String errorMsg = error.networkResponse != null
                                ? new String(error.networkResponse.data)
                                : error.toString();

                        Intent intent = new Intent(PinActivity.this, DeclineActivity.class);
                        intent.putExtra("reason", errorMsg);
                        startActivity(intent);
                        finish();
                    }
            );

            queue.add(request);

        } catch (Exception e) {
            progressDialog.dismiss();
            e.printStackTrace();
        }
    }

    private void showErrorDialog(String message) {

        new AlertDialog.Builder(PinActivity.this)
                .setTitle("Transaction Failed")
                .setMessage(message)
                .setPositiveButton("Close", (dialog, which) -> dialog.dismiss())
                .show();
    }
}

package com.example.trustpay.ui.auth;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.trustpay.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    TextInputEditText etName, etEmail, etMobile, etPassword, etUpiPin, etBalance;
    MaterialButton btnRegister;

    String BASE_URL = "http://10.228.6.76:5000/register";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etMobile = findViewById(R.id.etMobile);
        etPassword = findViewById(R.id.etPassword);
        etUpiPin = findViewById(R.id.etUpiPin);
        etBalance = findViewById(R.id.etBalance); // ✅ NEW

        btnRegister = findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {

        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String mobile = etMobile.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String upiPin = etUpiPin.getText().toString().trim();
        String balance = etBalance.getText().toString().trim(); // ✅ NEW

        // 🔴 Check empty fields
        if (name.isEmpty() || email.isEmpty() || mobile.isEmpty() ||
                password.isEmpty() || upiPin.isEmpty() || balance.isEmpty()) {

            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        // 🔴 PIN validation
        if (upiPin.length() != 4) {
            Toast.makeText(this, "UPI PIN must be 4 digits", Toast.LENGTH_SHORT).show();
            return;
        }

        // 🔴 Balance validation
        try {
            Double.parseDouble(balance);
        } catch (Exception e) {
            Toast.makeText(this, "Enter valid balance", Toast.LENGTH_SHORT).show();
            return;
        }

        try {

            JSONObject jsonBody = new JSONObject();

            jsonBody.put("name", name);
            jsonBody.put("email", email);
            jsonBody.put("mobile", mobile);
            jsonBody.put("password", password);
            jsonBody.put("upi_pin", upiPin);
            jsonBody.put("balance", balance); // ✅ NEW FIELD

            RequestQueue queue = Volley.newRequestQueue(this);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    BASE_URL,
                    jsonBody,
                    response -> {

                        Toast.makeText(this,
                                "Registered Successfully\nUPI: " + response.optString("upi_id"),
                                Toast.LENGTH_LONG).show();

                    },
                    error -> {

                        // 🔥 Show backend error if available
                        String message = "Registration Failed";
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            try {
                                String errorData = new String(error.networkResponse.data);
                                message = errorData;
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    }) {

                // ✅ IMPORTANT HEADER
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };

            queue.add(request);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
package com.example.trustpay.ui.profile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.trustpay.R;
import com.example.trustpay.network.BackendConfig;
import com.example.trustpay.ui.auth.LoginActivity;
import com.google.android.material.button.MaterialButton;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    TextView tvName, tvEmail, tvMobile, tvUpi, tvBalance;
    MaterialButton btnShowQr, btnLogout;

    String BASE_URL = BackendConfig.endpoint("balance/");
    private String currentUpi = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        tvName = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        tvMobile = findViewById(R.id.tvMobile);
        tvUpi = findViewById(R.id.tvUpi);
        tvBalance = findViewById(R.id.tvBalance);
        btnShowQr = findViewById(R.id.btnShowQr);
        btnLogout = findViewById(R.id.btnLogout);

        SharedPreferences prefs = getSharedPreferences("user_data", MODE_PRIVATE);

        if (!prefs.getBoolean("is_logged_in", false)) {
            openLoginPage();
            return;
        }

        String name = prefs.getString("name", "");
        String email = prefs.getString("email", "");
        String mobile = prefs.getString("mobile", "");
        String upi = prefs.getString("upi", "");
        currentUpi = upi;

        tvName.setText("Name: " + name);
        tvEmail.setText("Email: " + email);
        tvMobile.setText("Mobile: " + mobile);
        tvUpi.setText("UPI ID: " + upi);

        fetchBalance(upi);

        btnShowQr.setOnClickListener(v -> showQrDialog(name, mobile, upi));

        btnLogout.setOnClickListener(v -> {
            prefs.edit().clear().apply();
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
            openLoginPage();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentUpi != null && !currentUpi.isEmpty()) {
            fetchBalance(currentUpi);
        }
    }

    private void openLoginPage() {
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void showQrDialog(String name, String mobile, String upi) {
        if (upi == null || upi.isEmpty()) {
            Toast.makeText(this, "UPI ID missing", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Bitmap qrBitmap = createQrBitmap(createQrPayload(name, mobile, upi), 720);

            LinearLayout content = new LinearLayout(this);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setGravity(Gravity.CENTER_HORIZONTAL);
            int padding = dp(20);
            content.setPadding(padding, padding, padding, padding);

            ImageView qrImage = new ImageView(this);
            qrImage.setImageBitmap(qrBitmap);
            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(dp(260), dp(260));
            content.addView(qrImage, imageParams);

            TextView upiText = new TextView(this);
            upiText.setText(upi);
            upiText.setTextSize(18);
            upiText.setTextColor(Color.BLACK);
            upiText.setGravity(Gravity.CENTER);
            upiText.setPadding(0, dp(14), 0, 0);
            content.addView(upiText);

            new AlertDialog.Builder(this)
                    .setTitle("TrustPay QR")
                    .setView(content)
                    .setPositiveButton("Close", (dialog, which) -> dialog.dismiss())
                    .show();
        } catch (Exception e) {
            Toast.makeText(this, "Unable to generate QR", Toast.LENGTH_SHORT).show();
        }
    }

    private String createQrPayload(String name, String mobile, String upi) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("type", "trustpay_user");
        payload.put("name", name);
        payload.put("mobile", mobile);
        payload.put("upi", upi);
        return payload.toString();
    }

    private Bitmap createQrBitmap(String value, int size) throws Exception {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.MARGIN, 1);

        BitMatrix bitMatrix = new MultiFormatWriter().encode(
                value,
                BarcodeFormat.QR_CODE,
                size,
                size,
                hints
        );

        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
        }
        return bitmap;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private void fetchBalance(String upi) {
        String url = BASE_URL + upi;

        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    String balance = response.optString("balance");
                    tvBalance.setText("Balance: Rs. " + balance);
                },
                error -> Toast.makeText(this, "Failed to fetch balance", Toast.LENGTH_SHORT).show()
        );

        queue.add(request);
    }
}

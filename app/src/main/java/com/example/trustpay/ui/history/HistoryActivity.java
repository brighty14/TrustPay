package com.example.trustpay.ui.history;

import android.os.Bundle;
import android.content.SharedPreferences;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trustpay.R;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.example.trustpay.network.BackendConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import android.util.Log;
import android.widget.Toast;

public class HistoryActivity extends AppCompatActivity {

    private static final SimpleDateFormat HISTORY_TIMESTAMP_FORMAT =
            new SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.ENGLISH);

    RecyclerView recyclerView;
    HistoryAdapter adapter;
    List<Transaction> transactionList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history);

        recyclerView = findViewById(R.id.recyclerHistory);

        transactionList = new ArrayList<>();

        SharedPreferences prefs = getSharedPreferences("user_data", MODE_PRIVATE);
        String upi = prefs.getString("upi", "");

        Toast.makeText(this, "History UPI: " + upi, Toast.LENGTH_LONG).show();

        adapter = new HistoryAdapter(transactionList, upi);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        fetchTransactionHistory();
    }

    private void fetchTransactionHistory() {

        SharedPreferences prefs = getSharedPreferences("user_data", MODE_PRIVATE);
        String upiId = prefs.getString("upi", "");

        if (upiId == null || upiId.isEmpty()) {
            Toast.makeText(this, "UPI missing", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = BackendConfig.endpoint("transactions/" + upiId);

        RequestQueue queue = Volley.newRequestQueue(this);

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,

                response -> {

                    transactionList.clear();

                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject obj = response.getJSONObject(i);

                            String sender = obj.optString("sender_upi");
                            String receiver = obj.optString("receiver_upi");
                            double amount = obj.optDouble("amount");
                            String status = obj.optString("status");
                            String timestamp = obj.optString("timestamp");

                            Transaction t = new Transaction(sender, receiver, amount, status, timestamp);
                            transactionList.add(t);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    Collections.sort(transactionList, new Comparator<Transaction>() {
                        @Override
                        public int compare(Transaction first, Transaction second) {
                            Date firstDate = parseHistoryTimestamp(first.getTimestamp());
                            Date secondDate = parseHistoryTimestamp(second.getTimestamp());

                            if (firstDate == null && secondDate == null) {
                                return 0;
                            }
                            if (firstDate == null) {
                                return 1;
                            }
                            if (secondDate == null) {
                                return -1;
                            }

                            return secondDate.compareTo(firstDate);
                        }
                    });

                    adapter.notifyDataSetChanged();

                    if (transactionList.isEmpty()) {
                        Toast.makeText(this, "No transactions found", Toast.LENGTH_SHORT).show();
                    }
                },

                error -> {
                    Toast.makeText(this, "API Error", Toast.LENGTH_SHORT).show();
                }
        );

        queue.add(request);
    }

    private Date parseHistoryTimestamp(String timestamp) {
        try {
            return HISTORY_TIMESTAMP_FORMAT.parse(timestamp);
        } catch (ParseException e) {
            Log.w("HistoryActivity", "Unable to parse transaction timestamp: " + timestamp, e);
            return null;
        }
    }
}

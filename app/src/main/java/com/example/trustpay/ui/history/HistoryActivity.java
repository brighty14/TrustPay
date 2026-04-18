package com.example.trustpay.ui.history;

import android.os.Bundle;
import android.content.SharedPreferences;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trustpay.R;
import java.util.ArrayList;
import java.util.List;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import android.util.Log;
import android.widget.Toast;

public class HistoryActivity extends AppCompatActivity {

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

        String url = "http://10.41.17.76:5000/transactions/" + upiId;

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
}

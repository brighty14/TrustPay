package com.example.trustpay.ui.history;

import android.view.LayoutInflater;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trustpay.R;
import com.example.trustpay.ui.history.Transaction;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    List<Transaction> list;
    String currentUpi;

    public HistoryAdapter(List<Transaction> list, String currentUpi) {
        this.list = list;
        this.currentUpi = currentUpi;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvAmount, tvStatus, tvDate, tvReceiver;

        public ViewHolder(View itemView) {
            super(itemView);

            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvReceiver = itemView.findViewById(R.id.tvReceiver);
        }
    }

    @NonNull
    @Override
    public HistoryAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryAdapter.ViewHolder holder, int position) {

        Transaction t = list.get(position);

        holder.tvAmount.setText("₹ " + t.getAmount());
        holder.tvDate.setText(t.getTimestamp());

// 🔥 LOGIC: SENT vs RECEIVED
        if (t.getSenderUpi().equals(currentUpi)) {

            // 👉 SENT
            holder.tvReceiver.setText("To: " + t.getReceiverUpi());
            holder.tvStatus.setText("↑ Sent");


            holder.tvAmount.setTextColor(android.graphics.Color.parseColor("#D32F2F")); // red
            holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#D32F2F"));

        } else {

            // 👉 RECEIVED
            holder.tvReceiver.setText("From: " + t.getSenderUpi());
            holder.tvStatus.setText("↓ Received");

            holder.tvAmount.setTextColor(android.graphics.Color.parseColor("#388E3C")); // green
            holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#388E3C"));
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}
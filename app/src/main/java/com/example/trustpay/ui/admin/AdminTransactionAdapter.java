package com.example.trustpay.ui.admin;

import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trustpay.R;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class AdminTransactionAdapter extends RecyclerView.Adapter<AdminTransactionAdapter.ViewHolder> {

    private List<AdminTransaction> list;

    public AdminTransactionAdapter(List<AdminTransaction> list) {
        this.list = list;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSender, tvReceiver, tvAmount, tvStatus, tvDate, tvAnomalyLabel, tvRiskScore;
        MaterialCardView cardView;

        public ViewHolder(View itemView) {
            super(itemView);
            tvSender = itemView.findViewById(R.id.tvAdminSender);
            tvReceiver = itemView.findViewById(R.id.tvAdminReceiver);
            tvAmount = itemView.findViewById(R.id.tvAdminAmount);
            tvStatus = itemView.findViewById(R.id.tvAdminStatus);
            tvDate = itemView.findViewById(R.id.tvAdminDate);
            tvAnomalyLabel = itemView.findViewById(R.id.tvAnomalyLabel);
            tvRiskScore = itemView.findViewById(R.id.tvRiskScore);
            cardView = itemView.findViewById(R.id.cardAdminTransaction);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AdminTransaction t = list.get(position);

        holder.tvSender.setText("From: " + t.getSenderUpi());
        holder.tvReceiver.setText("To: " + t.getReceiverUpi());
        holder.tvAmount.setText("₹ " + t.getAmount());
        holder.tvDate.setText(t.getTimestamp());
        holder.tvStatus.setText(t.getStatus());

        holder.tvRiskScore.setText("Risk: " + t.getRiskScore() + " (" + t.getRiskLevel() + ")");

        if ("High".equalsIgnoreCase(t.getRiskLevel())) {
            holder.tvRiskScore.setTextColor(Color.parseColor("#D32F2F")); // Red
            holder.cardView.setStrokeColor(Color.parseColor("#D32F2F"));
            holder.cardView.setStrokeWidth(3);
        } else if ("Medium".equalsIgnoreCase(t.getRiskLevel())) {
            holder.tvRiskScore.setTextColor(Color.parseColor("#F57C00")); // Orange
            holder.cardView.setStrokeColor(Color.parseColor("#F57C00"));
            holder.cardView.setStrokeWidth(3);
        } else {
            holder.tvRiskScore.setTextColor(Color.parseColor("#388E3C")); // Green
            holder.cardView.setStrokeWidth(0);
        }

        if (t.isAnomaly() || "FAILED".equals(t.getStatus())) {
            holder.cardView.setCardBackgroundColor(Color.parseColor("#FFEBEE")); // Light red
            holder.tvStatus.setTextColor(Color.parseColor("#D32F2F")); // Red
            if (t.isAnomaly()) {
                holder.tvAnomalyLabel.setVisibility(View.VISIBLE);
            } else {
                holder.tvAnomalyLabel.setVisibility(View.GONE);
            }
        } else {
            holder.cardView.setCardBackgroundColor(Color.WHITE);
            holder.tvStatus.setTextColor(Color.parseColor("#388E3C")); // Green
            holder.tvAnomalyLabel.setVisibility(View.GONE);
        }

        holder.cardView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), AdminUserProfileActivity.class);
            intent.putExtra("upi_id", t.getSenderUpi());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}

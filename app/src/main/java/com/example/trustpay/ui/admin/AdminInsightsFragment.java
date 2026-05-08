package com.example.trustpay.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.trustpay.R;

public class AdminInsightsFragment extends Fragment {

    private TextView tvTotalTxns, tvTotalAnomalies, tvFraudRate, tvAlerts;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_insights, container, false);
        tvTotalTxns = view.findViewById(R.id.tvTotalTxns);
        tvTotalAnomalies = view.findViewById(R.id.tvTotalAnomalies);
        tvFraudRate = view.findViewById(R.id.tvFraudRate);
        tvAlerts = view.findViewById(R.id.tvAlerts);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof AdminDashboardActivity) {
            AdminDashboardActivity activity = (AdminDashboardActivity) getActivity();
            tvTotalTxns.setText(String.valueOf(activity.totalTxnsToday));
            tvTotalAnomalies.setText(String.valueOf(activity.anomalyCount));
            tvFraudRate.setText(activity.fraudRate + "%");
            if (activity.alertMessage != null && !activity.alertMessage.isEmpty()) {
                tvAlerts.setText("🚨 " + activity.alertMessage);
            }
        }
    }
}

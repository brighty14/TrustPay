package com.example.trustpay.ui.admin;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.trustpay.R;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import java.util.ArrayList;

public class AdminChartsFragment extends Fragment {

    private PieChart pieChart;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_charts, container, false);
        pieChart = view.findViewById(R.id.anomalyPieChart);
        setupChart();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof AdminDashboardActivity) {
            AdminDashboardActivity activity = (AdminDashboardActivity) getActivity();
            updateChart(activity.normalCount, activity.anomalyCount);
        }
    }

    private void setupChart() {
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setTransparentCircleRadius(61f);
        pieChart.setCenterText("Transactions");
        pieChart.setCenterTextSize(16f);
    }

    private void updateChart(int normalCount, int anomalyCount) {
        ArrayList<PieEntry> entries = new ArrayList<>();

        if (normalCount > 0) {
            entries.add(new PieEntry(normalCount, "Normal"));
        }
        if (anomalyCount > 0) {
            entries.add(new PieEntry(anomalyCount, "Anomaly"));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Transaction Status");
        
        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(Color.parseColor("#388E3C")); // Green
        colors.add(Color.parseColor("#D32F2F")); // Red
        dataSet.setColors(colors);

        PieData data = new PieData(dataSet);
        data.setValueTextSize(14f);
        data.setValueTextColor(Color.WHITE);

        pieChart.setData(data);
        pieChart.invalidate(); // refresh
    }
}

package com.example.trustpay.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trustpay.R;

public class AdminTransactionsFragment extends Fragment {

    private RecyclerView recyclerView;
    private AdminTransactionAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_transactions, container, false);
        recyclerView = view.findViewById(R.id.recyclerAdminTransactions);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof AdminDashboardActivity) {
            AdminDashboardActivity activity = (AdminDashboardActivity) getActivity();
            if (adapter == null) {
                adapter = new AdminTransactionAdapter(activity.transactionList);
                recyclerView.setAdapter(adapter);
            } else {
                adapter.notifyDataSetChanged();
            }
        }
    }
}

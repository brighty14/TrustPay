package com.example.trustpay.network;

import com.example.trustpay.ui.history.Transaction;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ApiService {

    @GET("/transactions/{upi_id}")
    Call<List<Transaction>> getTransactions(@Path("upi_id") String upiId);
}
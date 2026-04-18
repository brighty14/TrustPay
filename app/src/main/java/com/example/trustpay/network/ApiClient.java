package com.example.trustpay.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    // Use your laptop's Wi-Fi IP when running on a real Android device.
    private static final String BASE_URL = "http://10.41.17.76:5000/";

    public static Retrofit getClient() {
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }
}

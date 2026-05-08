package com.example.trustpay.network;

public final class BackendConfig {

    // Update this host when the backend machine IP changes on your LAN.
    private static final String HOST = "10.55.218.152";
    private static final String PORT = "5000";
    public static final String BASE_URL = "http://" + HOST + ":" + PORT + "/";

    private BackendConfig() {
    }

    public static String endpoint(String path) {
        return BASE_URL + path;
    }
}

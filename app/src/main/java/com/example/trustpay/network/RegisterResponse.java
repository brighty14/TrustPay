package com.example.trustpay.network;

public class RegisterResponse {

    private String status;
    private String message;
    private String upi_id;
    private String user_id;

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getUpiId() {
        return upi_id;
    }

    public String getUserId() {
        return user_id;
    }
}

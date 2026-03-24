package com.example.trustpay.ui.history;

import java.util.Date;

public class Transaction {

    String sender_upi;
    String receiver_upi;
    double amount;
    String status;
    String timestamp;

    public Transaction(String sender_upi, String receiver_upi,
                       double amount, String status, String timestamp) {
        this.sender_upi = sender_upi;
        this.receiver_upi = receiver_upi;
        this.amount = amount;
        this.status = status;
        this.timestamp = timestamp;
    }

    public String getSenderUpi() {
        return sender_upi;
    }

    public String getReceiverUpi() {
        return receiver_upi;
    }

    public double getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
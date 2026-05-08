package com.example.trustpay.ui.admin;

public class AdminTransaction {
    private String senderUpi;
    private String receiverUpi;
    private double amount;
    private String status;
    private String timestamp;
    private boolean isAnomaly;
    private int riskScore;
    private String riskLevel;

    public AdminTransaction(String senderUpi, String receiverUpi, double amount, String status, String timestamp, boolean isAnomaly, int riskScore, String riskLevel) {
        this.senderUpi = senderUpi;
        this.receiverUpi = receiverUpi;
        this.amount = amount;
        this.status = status;
        this.timestamp = timestamp;
        this.isAnomaly = isAnomaly;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
    }

    public String getSenderUpi() { return senderUpi; }
    public String getReceiverUpi() { return receiverUpi; }
    public double getAmount() { return amount; }
    public String getStatus() { return status; }
    public String getTimestamp() { return timestamp; }
    public boolean isAnomaly() { return isAnomaly; }
    public int getRiskScore() { return riskScore; }
    public String getRiskLevel() { return riskLevel; }
}

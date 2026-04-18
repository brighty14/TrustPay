package com.example.trustpay.network;

public class RegisterRequest {

    private final String username;
    private final String name;
    private final String email;
    private final String mobile;
    private final String password;
    private final String upi_pin;
    private final double balance;
    private final String face_image;
    private final String front_face_image;
    private final String left_face_image;
    private final String right_face_image;

    public RegisterRequest(
            String username,
            String email,
            String mobile,
            String password,
            String upiPin,
            double balance,
            String faceImage
    ) {
        this.username = username;
        this.name = username;
        this.email = email;
        this.mobile = mobile;
        this.password = password;
        this.upi_pin = upiPin;
        this.balance = balance;
        this.face_image = faceImage;
        this.front_face_image = faceImage;
        this.left_face_image = faceImage;
        this.right_face_image = faceImage;
    }

    public RegisterRequest(
            String username,
            String email,
            String mobile,
            String password,
            String upiPin,
            double balance,
            String frontFaceImage,
            String leftFaceImage,
            String rightFaceImage
    ) {
        this.username = username;
        this.name = username;
        this.email = email;
        this.mobile = mobile;
        this.password = password;
        this.upi_pin = upiPin;
        this.balance = balance;
        this.face_image = frontFaceImage;
        this.front_face_image = frontFaceImage;
        this.left_face_image = leftFaceImage;
        this.right_face_image = rightFaceImage;
    }
}

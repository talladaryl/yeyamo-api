package com.yeyamo_mobile.api.auth_service.security;

public enum AntiBotAction {
    REGISTER("register"),
    REQUEST_OTP("request_otp"),
    RESEND_OTP("resend_otp"),
    FORGOT_PASSWORD("forgot_password"),
    LOGIN("login");

    private final String value;

    AntiBotAction(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}

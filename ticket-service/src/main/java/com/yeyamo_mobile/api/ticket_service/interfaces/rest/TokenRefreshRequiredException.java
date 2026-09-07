package com.yeyamo_mobile.api.ticket_service.interfaces.rest;

final class TokenRefreshRequiredException extends RuntimeException {
    TokenRefreshRequiredException() {
        super("Refresh the session before retrying this payment");
    }
}

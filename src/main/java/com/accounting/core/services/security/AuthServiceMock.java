package com.accounting.core.services.security;

public interface AuthServiceMock {
    AuthenticationResult login(String username);
    AuthenticationResult refreshToken(String refreshToken);
    void revokeToken();
    void changePassword();

    record AuthenticationResult(String idToken, String refreshToken) {
    }
}
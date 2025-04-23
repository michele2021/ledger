package com.accounting.core.services.security;

import com.fasterxml.jackson.databind.JsonNode;

public interface AuthServiceMock {
    AuthenticationResult login(JsonNode loginInfo);
    AuthenticationResult refreshToken(String refreshToken);
    void revokeToken();
    void changePassword();

    record AuthenticationResult(String accessToken, String refreshToken) {
    }
}
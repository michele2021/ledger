package com.accounting.core.services;

import com.fasterxml.jackson.databind.JsonNode;

public interface AuthServiceMock {
    AuthServiceMockImpl.AuthenticationResult login(JsonNode jsonNode);
    AuthServiceMockImpl.AuthenticationResult refreshToken(String userId);
    void revokeToken();
    void changePassword();
}

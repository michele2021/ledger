package com.accounting.core.services;

public interface AuthServiceMock {
    AuthServiceMockImpl.AuthenticationResult login(String userId);
    AuthServiceMockImpl.AuthenticationResult refreshToken(String userId);
    void revokeToken();
    void changePassword();
}

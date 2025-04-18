package com.accounting.core.services;

import com.accounting.commons.resources.security.AuthUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AuthServiceMockImpl implements AuthServiceMock {

    private final AuthUtils authUtils;
    private String refreshToken;

    public record AuthenticationResult(String accessToken, String refreshToken, String idToken) {
    }

    public AuthServiceMockImpl() {
        this.authUtils = new AuthUtils();
    }

    @Override
    public AuthenticationResult login(String userId) {
        var accessToken = authUtils.token(userId, "DEFAULT");
        log.info("mock login! access token: {}", accessToken);
        this.refreshToken = authUtils.refreshToken(userId);
        return new AuthenticationResult(accessToken, "dummy-refresh-token-123", accessToken);
    }

    @Override
    public AuthenticationResult refreshToken(String userId) {
        log.info("mock refresh-token!");
        var accessToken = authUtils.token(userId, "DEFAULT");
        return new AuthenticationResult(accessToken, refreshToken, "dummy-id-token-789");
    }

    @Override
    public void revokeToken() {
        log.info("mock revoke-token!");
        // nothing to do
    }

    @Override
    public void changePassword() {
        log.info("mock change password!");
        // nothing to do
    }
}

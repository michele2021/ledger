package com.accounting.core.services;

import com.accounting.commons.resources.security.HMACAuthEncoder;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class AuthServiceMockImpl implements AuthServiceMock {

    private final HMACAuthEncoder hmacAuthUtil;
    private String refreshToken;
    private Map<String, String> userRoles;

    public record AuthenticationResult(String accessToken, String refreshToken, String idToken) {
    }

    public AuthServiceMockImpl() {
        this.hmacAuthUtil = HMACAuthEncoder.getInstance();
        this.userRoles = Map.of("mic", "SUPER_USER", "pradipta", "SUPER_USER");
    }

    @Override
    public AuthenticationResult login(JsonNode jsonNode) {
        var userName = jsonNode.get("username").asText();
        var clientId = jsonNode.get("clientId").asText();
        var role = userRoles.get(userName) == null ? "DEFAULT" : userRoles.get(userName);
        log.info("AuthServiceMockImpl username: {}, clientId {}, role {}.", userName, clientId, role);
        var accessToken = hmacAuthUtil.createAccessToken(userName, role);
        this.refreshToken = hmacAuthUtil.createRefreshToken(userName);
        return new AuthenticationResult(accessToken, "dummy-refresh-token-123", accessToken);
    }

    @Override
    public AuthenticationResult refreshToken(String userId) {
        log.info("mock refresh-token!");
        var accessToken = hmacAuthUtil.createAccessToken(userId, "DEFAULT");
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

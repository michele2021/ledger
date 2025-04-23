package com.accounting.core.services.security;

import com.accounting.commons.resources.security.RSAAuthDecoder;
import com.accounting.commons.resources.security.RSAAuthEncoder;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class AuthServiceMockImpl implements AuthServiceMock {
    private final RSAAuthEncoder rsaAuthEncoder;
    private final RSAAuthDecoder rsaAuthDecoder;
    private final Map<String, String> userRoles;

    @Inject
    public AuthServiceMockImpl(RSAAuthEncoder rsaAuthEncoder, RSAAuthDecoder rsaAuthDecoder) {
        this.rsaAuthEncoder = rsaAuthEncoder;
        this.rsaAuthDecoder = rsaAuthDecoder;
        this.userRoles = Map.of("mic", "SUPER_USER", "pradipta", "SUPER_USER");
    }

    @Override
    public AuthenticationResult login(JsonNode loginInfo) {
        var userName = loginInfo.get("username").asText();
        var clientId = loginInfo.get("clientId").asText();
        var role = userRoles.get(userName) == null ? "DEFAULT" : userRoles.get(userName);
        log.info("AuthServiceMockImpl username: {}, clientId {}, role {}.", userName, clientId, role);
        var accessToken = rsaAuthEncoder.createAccessToken(userName, role);
        var refreshToken = rsaAuthEncoder.createRefreshToken(userName); // TO be modified (not JWT)
        return new AuthenticationResult(accessToken, refreshToken);
    }

    @Override
    public AuthenticationResult refreshToken(String refreshToken) {
        log.info("mock refresh-token!");
        String userId = rsaAuthDecoder.getAuthUserName(refreshToken);
        if (userId == null) {
            return null;
        }
        var accessToken = rsaAuthEncoder.createAccessToken(userId, "DEFAULT");
        return new AuthenticationResult(accessToken, refreshToken);
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

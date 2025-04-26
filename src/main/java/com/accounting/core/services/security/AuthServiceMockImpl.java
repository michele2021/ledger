package com.accounting.core.services.security;

import com.accounting.commons.entities.User;
import com.accounting.commons.repository.UserRepoIf;
import com.accounting.commons.resources.security.RSAAuthDecoder;
import com.accounting.commons.resources.security.RSAAuthEncoder;
import com.google.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class AuthServiceMockImpl implements AuthServiceMock {
    private final UserRepoIf userRepo;
    private final RSAAuthEncoder rsaAuthEncoder;
    private final RSAAuthDecoder rsaAuthDecoder;

    @Override
    public AuthenticationResult login(String username) {
        User user = userRepo.get(username);
        if(user == null) {
            throw new WebApplicationException("User not found.", Response.Status.BAD_REQUEST);
        }
        var userId = String.valueOf(user.getUserId());
        // N.B. initially we assume 1 user - 1 company
        var companyRole = user.getCompanyRoles().getFirst();
        log.info("AuthServiceMockImpl user retrieved from db {}", user);
        // N.B. we could also store the Company accountId in the token (we do not do it for now in case we want in the
        // future this AuthService to be isolated and do not have knowledge about companies/clients).
        var accessToken = rsaAuthEncoder.createAccessToken(userId, companyRole.role().name());
        var refreshToken = rsaAuthEncoder.createRefreshToken(userId); // TO be modified (not JWT)
        return new AuthenticationResult(accessToken, refreshToken);
    }

    @Override
    public AuthenticationResult refreshToken(String refreshToken) {
        log.info("mock refresh-token!!!!! {}", refreshToken);
        var user = rsaAuthDecoder.getUser(refreshToken);
        if (user == null) {
            return null;
        }
        log.info("mock refresh-token!!!!! userId {}", user.userId());
        // N.B. for now we hardcode "DEFAULT" as a role since this claim of the token is not used anyway.
        var accessToken = rsaAuthEncoder.createAccessToken(String.valueOf(user.userId()), "DEFAULT");
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

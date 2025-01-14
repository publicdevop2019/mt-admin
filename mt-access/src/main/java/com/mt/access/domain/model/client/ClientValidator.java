package com.mt.access.domain.model.client;


import com.mt.common.domain.model.validate.Checker;
import com.mt.common.domain.model.validate.ValidationNotificationHandler;

public class ClientValidator {
    private final Client client;
    private final ValidationNotificationHandler handler;

    public ClientValidator(Client client, ValidationNotificationHandler handler) {
        this.client = client;
        this.handler = handler;
    }

    protected void validate() {
        accessAndType();
        encryptedSecret();
        tokenAndGrantType();
        redirectAndGrantType();
        pathAndType();
        externalUrlAndType();
    }

    private void redirectAndGrantType() {
        if (
            client.getGrantTypes().contains(GrantType.AUTHORIZATION_CODE)
                && client.getRedirectUrls().isEmpty()
        ) {
            handler
                .handleError("redirect details and authorization grant must both exist");
        }
        if (
            !client.getGrantTypes().contains(GrantType.AUTHORIZATION_CODE)
                && !client.getRedirectUrls().isEmpty()
        ) {
            handler
                .handleError("redirect details and authorization grant must both exist");
        }
    }

    private void tokenAndGrantType() {
        if (client.getGrantTypes() != null && !client.getGrantTypes().isEmpty()) {
            if (client.getTokenDetail().getAccessTokenValiditySeconds() == null
                ||
                client.getTokenDetail().getAccessTokenValiditySeconds() < 60) {
                handler
                    .handleError("when grant present access token validity seconds must be valid");
            }
            if (client.getGrantTypes().contains(GrantType.REFRESH_TOKEN)) {
                if (client.getTokenDetail().getRefreshTokenValiditySeconds() == null
                    ||
                    client.getTokenDetail().getRefreshTokenValiditySeconds() < 120) {
                    handler
                        .handleError("refresh grant must has valid refresh token validity seconds");
                }
            } else {
                if (client.getTokenDetail().getRefreshTokenValiditySeconds() != null) {
                    handler
                        .handleError("refresh token validity seconds requires refresh grant");
                }
            }
        }
    }

    private void pathAndType() {
        if (Checker.isBlank(client.getPath())
            &&
            client.getTypes().contains(ClientType.BACKEND_APP)) {
            handler.handleError("backend client require path");
        }
        if (!Checker.isBlank(client.getPath())
            &&
            client.getTypes().contains(ClientType.FRONTEND_APP)) {
            handler.handleError("frontend client should not have path");
        }
    }

    private void externalUrlAndType() {
        if (client.getExternalUrl() == null
            &&
            client.getTypes().contains(ClientType.BACKEND_APP)) {
            handler.handleError("backend client require external url");
        }
        if (client.getExternalUrl() != null
            &&
            client.getTypes().contains(ClientType.FRONTEND_APP)) {
            handler.handleError("frontend client should not external url");
        }
    }

    private void encryptedSecret() {
        if (client.getSecret() == null) {
            if (client.getTypes().stream().noneMatch(e -> e.equals(ClientType.FRONTEND_APP))) {
                handler.handleError("client secret required");
            }
        }
    }

    private void accessAndType() {
        if (client.getAccessible() != null && client.getAccessible()) {
            if (
                client.getTypes().stream().anyMatch(e -> e.equals(ClientType.FRONTEND_APP))
            ) {
                handler.handleError(
                    "invalid client type to be a resource, "
                        +
                        "must be backend application");
            }
        }
        if (client.getTypes().stream().anyMatch(e -> e.equals(ClientType.FRONTEND_APP))) {
            if (client.getAccessible() != null) {
                handler.handleError(
                    "only backend can specify accessible"
                );
            }
        }
        if (client.getTypes().stream().anyMatch(e -> e.equals(ClientType.BACKEND_APP))) {
            if (client.getAccessible() == null) {
                handler.handleError(
                    "backend must specify accessible"
                );
            }
        }
    }
}

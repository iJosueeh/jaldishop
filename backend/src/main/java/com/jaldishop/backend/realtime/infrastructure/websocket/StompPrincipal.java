package com.jaldishop.backend.realtime.infrastructure.websocket;

import com.jaldishop.backend.shared.security.JwtPrincipal;

import java.security.Principal;
import java.util.Objects;

public record StompPrincipal(
        String name,
        JwtPrincipal jwtPrincipal
) implements Principal {

    public StompPrincipal {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(jwtPrincipal, "jwtPrincipal must not be null");
    }

    @Override
    public String getName() {
        return name;
    }

}

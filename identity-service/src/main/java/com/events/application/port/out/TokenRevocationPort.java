package com.events.application.port.out;

import java.time.Instant;

public interface TokenRevocationPort {
    void revoke(String token, Instant expiresAt);
    boolean isRevoked(String token);
}

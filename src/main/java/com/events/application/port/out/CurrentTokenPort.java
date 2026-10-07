package com.events.application.port.out;

import java.time.Instant;

/** Token autenticado de la peticion, sin depender de JWT ni Spring Security. */
public interface CurrentTokenPort {
    String tokenValue();
    Instant expiresAt();
}

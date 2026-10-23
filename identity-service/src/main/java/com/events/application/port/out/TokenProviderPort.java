package com.events.application.port.out;

import com.events.domain.entity.Usuario;

/**
 * Emite el token de acceso (JWT en infraestructura) para un usuario autenticado.
 */
public interface TokenProviderPort {
    String generate(Usuario usuario);

    long expirationSeconds();
}

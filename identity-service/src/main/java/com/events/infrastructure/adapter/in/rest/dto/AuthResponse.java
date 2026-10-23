package com.events.infrastructure.adapter.in.rest.dto;

/**
 * accessToken se envia en cada peticion protegida como "Authorization: Bearer {accessToken}".
 */
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UsuarioResponse usuario
) {
}

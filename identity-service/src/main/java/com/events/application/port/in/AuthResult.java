package com.events.application.port.in;

import com.events.domain.entity.Usuario;

/**
 * Resultado de registrarse o iniciar sesion: el token de acceso emitido, sus segundos de
 * vigencia y el usuario autenticado.
 */
public record AuthResult(String accessToken, long expiresInSeconds, Usuario usuario) {
}

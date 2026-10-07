package com.events.domain.entity;

import java.nio.charset.StandardCharsets;

public final class PasswordPolicy {
    private PasswordPolicy() { }
    public static void validar(String password) {
        if (password == null || password.isBlank() || password.codePointCount(0, password.length()) < 8
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("La contrasena debe tener al menos 8 caracteres y un maximo de 72 bytes UTF-8.");
        }
    }
}

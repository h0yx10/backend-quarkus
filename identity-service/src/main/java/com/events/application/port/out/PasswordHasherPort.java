package com.events.application.port.out;

/**
 * Abstrae el algoritmo de hash de passwords (BCrypt en infraestructura) para que los casos de
 * uso no dependan de Spring Security.
 */
public interface PasswordHasherPort {
    String hash(String rawPassword);

    boolean matches(String rawPassword, String passwordHash);
}

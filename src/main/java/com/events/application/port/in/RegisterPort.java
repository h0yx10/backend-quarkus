package com.events.application.port.in;

public interface RegisterPort {
    AuthResult execute(String nombre, String correo, String password);
}

package com.events.application.port.in;

public interface LoginPort {
    AuthResult execute(String correo, String password);
}

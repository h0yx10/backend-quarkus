package com.events.infrastructure.security;
import com.events.application.port.out.PasswordHasherPort;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
@ApplicationScoped
public class BCryptPasswordHasherAdapter implements PasswordHasherPort {
    public String hash(String password) { return BcryptUtil.bcryptHash(password,10); }
    public boolean matches(String password,String hash) { return BcryptUtil.matches(password,hash); }
}

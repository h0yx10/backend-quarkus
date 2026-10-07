package com.events.application.usecase;

import com.events.application.port.in.LogoutPort;
import com.events.application.port.out.CurrentTokenPort;
import com.events.application.port.out.TokenRevocationPort;

public class LogoutUseCase implements LogoutPort {
    private final CurrentTokenPort current;
    private final TokenRevocationPort revocations;

    public LogoutUseCase(CurrentTokenPort current, TokenRevocationPort revocations) {
        this.current = current;
        this.revocations = revocations;
    }

    @Override
    public void execute() {
        revocations.revoke(current.tokenValue(), current.expiresAt());
    }
}

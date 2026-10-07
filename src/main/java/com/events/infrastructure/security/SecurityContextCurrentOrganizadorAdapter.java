package com.events.infrastructure.security;

import com.events.application.port.out.*;
import java.util.UUID;
import io.quarkus.security.ForbiddenException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SecurityContextCurrentOrganizadorAdapter implements CurrentOrganizadorPort {
    private final CurrentUsuarioPort current;
    private final OrganizadorRepositoryPort organizadores;
    @Inject
    public SecurityContextCurrentOrganizadorAdapter(CurrentUsuarioPort current, OrganizadorRepositoryPort organizadores) {
        this.current = current; this.organizadores = organizadores;
    }
    @Override
    public UUID currentOrganizadorId() {
        return organizadores.findByUsuarioId(current.currentUsuarioId()).filter(o -> o.isActivo())
                .orElseThrow(() -> new ForbiddenException()).getId();
    }
}

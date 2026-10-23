package com.events.application.port.in;

import java.util.UUID;

/** Indica si un organizador tiene eventos o capacidades; identity-service lo usa antes de borrar cuentas. */
public interface CheckOrganizadorDataPort {
    boolean execute(UUID organizadorId);
}

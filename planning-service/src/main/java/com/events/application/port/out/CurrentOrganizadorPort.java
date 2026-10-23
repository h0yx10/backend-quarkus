package com.events.application.port.out;

import java.util.UUID;

/**
 * Resuelve el organizador autenticado que hace la peticion actual. Toda consulta o cambio
 * sobre eventos, subtareas y capacidad se filtra por este id, de modo que cada usuario solo
 * ve y modifica sus propios datos.
 */
public interface CurrentOrganizadorPort {
    UUID currentOrganizadorId();
}

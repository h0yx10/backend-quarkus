package com.events.application.port.out;

import java.util.UUID;

/**
 * Consulta al bounded context de planificacion (planning-service) si un organizador tiene
 * eventos o capacidades registradas. Identidad no lee esas tablas directamente.
 */
public interface PlanningDataPort {
    boolean organizadorTieneDatos(UUID organizadorId);
}

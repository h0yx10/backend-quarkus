package com.events.infrastructure.adapter.out.planning;

import com.events.application.port.out.PlanningDataPort;
import com.events.domain.exception.ServicioNoDisponibleException;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class PlanningDataRestAdapter implements PlanningDataPort {
    private final PlanningClient client;

    public PlanningDataRestAdapter(@RestClient PlanningClient client) {
        this.client = client;
    }

    @Override
    public boolean organizadorTieneDatos(UUID organizadorId) {
        try {
            return client.datos(organizadorId).tieneDatos();
        } catch (RuntimeException ex) {
            // Sin respuesta de planning no se puede garantizar que el borrado sea seguro.
            throw new ServicioNoDisponibleException("No pudimos verificar los datos del usuario. Intenta mas tarde.", ex);
        }
    }
}

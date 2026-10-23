package com.events.application.port.in;

import com.events.domain.entity.Evento;
import java.time.LocalDateTime;
import java.util.UUID;

public interface UpdateEventoPort {
    Evento execute(UUID id, String nombre, String tipo, String cliente, String contactoCliente,
                    LocalDateTime fechaHora, String lugar, LocalDateTime plazoLimite);
}

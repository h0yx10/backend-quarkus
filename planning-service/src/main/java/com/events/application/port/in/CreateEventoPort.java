package com.events.application.port.in;

import com.events.domain.entity.Evento;
import java.time.LocalDateTime;
import java.util.List;

public interface CreateEventoPort {
    Evento execute(String nombre, String tipo, String cliente, String contactoCliente,
                    LocalDateTime fechaHora, String lugar, LocalDateTime plazoLimite,
                    List<NuevaSubtareaData> subtareasIniciales);
}

package com.events.application.port.in;

import com.events.domain.entity.Evento;
import java.util.UUID;

public interface GetEventoPort {
    Evento execute(UUID id);
}

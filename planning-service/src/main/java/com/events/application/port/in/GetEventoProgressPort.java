package com.events.application.port.in;

import java.util.UUID;

public interface GetEventoProgressPort {
    EventoProgress execute(UUID eventoId);
}

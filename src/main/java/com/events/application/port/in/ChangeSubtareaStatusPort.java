package com.events.application.port.in;

import com.events.domain.entity.EstadoSubtarea;
import com.events.domain.entity.Subtarea;
import java.util.UUID;

public interface ChangeSubtareaStatusPort {
    Subtarea execute(UUID subtareaId, EstadoSubtarea estado, String nota);
}

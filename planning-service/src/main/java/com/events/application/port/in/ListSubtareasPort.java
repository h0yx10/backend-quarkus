package com.events.application.port.in;

import com.events.domain.entity.Subtarea;
import java.util.List;
import java.util.UUID;

public interface ListSubtareasPort {
    List<Subtarea> execute(UUID eventoId);
}

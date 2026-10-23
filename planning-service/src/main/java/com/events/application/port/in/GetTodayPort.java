package com.events.application.port.in;

import com.events.domain.entity.EstadoSubtarea;
import java.util.UUID;

public interface GetTodayPort {
    TodayGroups execute(UUID eventoIdFiltro, EstadoSubtarea estadoFiltro);
}

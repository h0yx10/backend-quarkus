package com.events.application.port.in;

import com.events.domain.entity.Subtarea;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface CreateSubtareaPort {
    Subtarea execute(UUID eventoId, String nombre, String descripcion, LocalDate fechaObjetivo, BigDecimal horasEstimadas);

    default Subtarea execute(UUID eventoId, String nombre, LocalDate fechaObjetivo, BigDecimal horasEstimadas) {
        return execute(eventoId, nombre, null, fechaObjetivo, horasEstimadas);
    }
}

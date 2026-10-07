package com.events.application.port.in;

import com.events.domain.entity.Subtarea;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Cubre la edicion general de una subtarea (US-03) y la reprogramacion de fecha/horas
 * (US-06), validando sobrecarga diaria (US-07) cuando la fecha o las horas cambian.
 */
public interface UpdateSubtareaPort {
    Subtarea execute(UUID subtareaId, String nombre, String descripcion, LocalDate fechaObjetivo, BigDecimal horasEstimadas);

    default Subtarea execute(UUID subtareaId, String nombre, LocalDate fechaObjetivo, BigDecimal horasEstimadas) {
        return execute(subtareaId, nombre, null, fechaObjetivo, horasEstimadas);
    }
}

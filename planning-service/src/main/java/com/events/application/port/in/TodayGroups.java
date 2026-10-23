package com.events.application.port.in;

import com.events.domain.entity.Subtarea;
import java.util.List;

/**
 * Resultado de la vista "Hoy" (US-04): subtareas no DONE agrupadas por vencidas,
 * para hoy y proximas, ya ordenadas segun la regla de prioridad (fecha y luego esfuerzo).
 */
public record TodayGroups(List<Subtarea> vencidas, List<Subtarea> paraHoy, List<Subtarea> proximas) {
    public static final String REGLA = "Se muestran primero las Vencidas, luego las de Hoy y luego las Proximas. "
            + "Dentro de cada grupo, ordenamos por fecha (la mas antigua o mas cercana primero) "
            + "y, si hay empate, por menor esfuerzo (horas estimadas).";
}

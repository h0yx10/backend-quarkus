package com.events.application.usecase;

import com.events.application.port.out.CapacidadDiariaRepositoryPort;
import com.events.application.port.out.SubtareaRepositoryPort;
import com.events.domain.entity.CapacidadDiaria;
import com.events.domain.exception.CapacityConflictException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Regla de sobrecarga diaria (US-07, US-12) compartida por los casos de uso que agregan horas
 * a un dia: las horas no DONE ya planificadas en la fecha, mas las nuevas, no pueden superar
 * el limite vigente del organizador (6h si no configuro uno).
 */
class CapacidadDiariaValidator {

    private final CapacidadDiariaRepositoryPort capacidadDiariaRepository;
    private final SubtareaRepositoryPort subtareaRepository;

    CapacidadDiariaValidator(CapacidadDiariaRepositoryPort capacidadDiariaRepository,
                             SubtareaRepositoryPort subtareaRepository) {
        this.capacidadDiariaRepository = capacidadDiariaRepository;
        this.subtareaRepository = subtareaRepository;
    }

    BigDecimal limiteVigente(UUID organizadorId) {
        return capacidadDiariaRepository.findCurrentByOrganizadorId(organizadorId)
                .map(CapacidadDiaria::getLimiteHoras)
                .orElse(CapacidadDiaria.LIMITE_POR_DEFECTO);
    }

    /** Horas que quedarian planificadas en la fecha; {@code excluirSubtareaId} evita contar dos veces la subtarea editada. */
    BigDecimal totalPlanificado(UUID organizadorId, LocalDate fecha, BigDecimal horasNuevas, UUID excluirSubtareaId) {
        return subtareaRepository.sumHorasPlanificadas(organizadorId, fecha, excluirSubtareaId).add(horasNuevas);
    }

    void validar(UUID organizadorId, LocalDate fecha, BigDecimal horasNuevas, UUID excluirSubtareaId) {
        validar(organizadorId, Map.of(fecha, horasNuevas), excluirSubtareaId);
    }

    void validar(UUID organizadorId, Map<LocalDate, BigDecimal> horasNuevasPorFecha, UUID excluirSubtareaId) {
        if (horasNuevasPorFecha.isEmpty()) {
            return;
        }
        BigDecimal limite = limiteVigente(organizadorId);
        for (Map.Entry<LocalDate, BigDecimal> entry : horasNuevasPorFecha.entrySet()) {
            BigDecimal total = totalPlanificado(organizadorId, entry.getKey(), entry.getValue(), excluirSubtareaId);
            if (total.compareTo(limite) > 0) {
                throw new CapacityConflictException(total, limite);
            }
        }
    }
}

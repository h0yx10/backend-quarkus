package com.events.application.usecase;

import com.events.application.port.in.CheckOverloadConflictPort;
import com.events.application.port.in.OverloadCheckResult;
import com.events.application.port.out.CurrentOrganizadorPort;
import com.events.application.port.out.CapacidadDiariaRepositoryPort;
import com.events.application.port.out.SubtareaRepositoryPort;
import com.events.domain.entity.CapacidadDiaria;
import com.events.domain.entity.Subtarea;
import com.events.domain.exception.SubtareaNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class CheckOverloadConflictUseCase implements CheckOverloadConflictPort {

    private final SubtareaRepositoryPort subtareaRepository;
    private final CapacidadDiariaRepositoryPort capacidadDiariaRepository;
    private final CurrentOrganizadorPort currentOrganizador;

    public CheckOverloadConflictUseCase(SubtareaRepositoryPort subtareaRepository,
                                         CapacidadDiariaRepositoryPort capacidadDiariaRepository,
                                         CurrentOrganizadorPort currentOrganizador) {
        this.subtareaRepository = subtareaRepository;
        this.capacidadDiariaRepository = capacidadDiariaRepository;
        this.currentOrganizador = currentOrganizador;
    }

    @Override
    public OverloadCheckResult execute(UUID subtareaId, LocalDate nuevaFecha, BigDecimal nuevasHoras) {
        Subtarea subtarea = subtareaRepository.findByIdAndOrganizadorId(subtareaId, currentOrganizador.currentOrganizadorId())
                .orElseThrow(() -> new SubtareaNotFoundException("No encontramos la subtarea solicitada."));

        LocalDate fechaEfectiva = nuevaFecha != null ? nuevaFecha : subtarea.getFechaObjetivo();
        BigDecimal horasEfectivas = nuevasHoras != null ? nuevasHoras : subtarea.getHorasEstimadas();
        UUID organizadorId = subtarea.getEvento().getOrganizador().getId();

        BigDecimal limite = capacidadDiariaRepository.findCurrentByOrganizadorId(organizadorId)
                .map(CapacidadDiaria::getLimiteHoras)
                .orElse(CapacidadDiaria.LIMITE_POR_DEFECTO);
        BigDecimal otrasHoras = subtareaRepository.sumHorasPlanificadas(organizadorId, fechaEfectiva, subtarea.getId());
        BigDecimal totalPlanificado = otrasHoras.add(horasEfectivas);

        boolean conflicto = totalPlanificado.compareTo(limite) > 0;
        BigDecimal exceso = conflicto ? totalPlanificado.subtract(limite) : BigDecimal.ZERO;
        return new OverloadCheckResult(conflicto, totalPlanificado, limite, exceso);
    }
}

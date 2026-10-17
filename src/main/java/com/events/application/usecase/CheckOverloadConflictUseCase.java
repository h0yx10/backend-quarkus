package com.events.application.usecase;

import com.events.application.port.in.CheckOverloadConflictPort;
import com.events.application.port.in.OverloadCheckResult;
import com.events.application.port.out.CurrentOrganizadorPort;
import com.events.application.port.out.CapacidadDiariaRepositoryPort;
import com.events.application.port.out.SubtareaRepositoryPort;
import com.events.domain.entity.EstadoSubtarea;
import com.events.domain.entity.Subtarea;
import com.events.domain.exception.SubtareaNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class CheckOverloadConflictUseCase implements CheckOverloadConflictPort {

    private final SubtareaRepositoryPort subtareaRepository;
    private final CapacidadDiariaValidator capacidadDiaria;
    private final CurrentOrganizadorPort currentOrganizador;

    public CheckOverloadConflictUseCase(SubtareaRepositoryPort subtareaRepository,
                                         CapacidadDiariaRepositoryPort capacidadDiariaRepository,
                                         CurrentOrganizadorPort currentOrganizador) {
        this.subtareaRepository = subtareaRepository;
        this.capacidadDiaria = new CapacidadDiariaValidator(capacidadDiariaRepository, subtareaRepository);
        this.currentOrganizador = currentOrganizador;
    }

    @Override
    public OverloadCheckResult execute(UUID subtareaId, LocalDate nuevaFecha, BigDecimal nuevasHoras) {
        Subtarea subtarea = subtareaRepository.findByIdAndOrganizadorId(subtareaId, currentOrganizador.currentOrganizadorId())
                .orElseThrow(() -> new SubtareaNotFoundException("No encontramos la subtarea solicitada."));

        LocalDate fechaEfectiva = nuevaFecha != null ? nuevaFecha : subtarea.getFechaObjetivo();
        BigDecimal horasEfectivas = nuevasHoras != null ? nuevasHoras : subtarea.getHorasEstimadas();
        UUID organizadorId = subtarea.getEvento().getOrganizador().getId();
        boolean hecha = subtarea.getEstado() == EstadoSubtarea.DONE;

        BigDecimal limite = capacidadDiaria.limiteVigente(organizadorId);
        BigDecimal totalPlanificado = capacidadDiaria.totalPlanificado(organizadorId, fechaEfectiva,
                hecha ? BigDecimal.ZERO : horasEfectivas, subtarea.getId());

        boolean conflicto = !hecha && totalPlanificado.compareTo(limite) > 0;
        BigDecimal exceso = conflicto ? totalPlanificado.subtract(limite) : BigDecimal.ZERO;
        return new OverloadCheckResult(conflicto, totalPlanificado, limite, exceso);
    }
}

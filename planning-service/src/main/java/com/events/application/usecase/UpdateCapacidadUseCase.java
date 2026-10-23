package com.events.application.usecase;

import com.events.application.port.in.UpdateCapacidadPort;
import com.events.application.port.out.CapacidadDiariaRepositoryPort;
import com.events.application.port.out.CurrentOrganizadorPort;
import com.events.domain.entity.CapacidadDiaria;
import java.math.BigDecimal;
import java.time.LocalDate;

public class UpdateCapacidadUseCase implements UpdateCapacidadPort {

    private final CapacidadDiariaRepositoryPort capacidadDiariaRepository;
    private final CurrentOrganizadorPort currentOrganizador;

    public UpdateCapacidadUseCase(CapacidadDiariaRepositoryPort capacidadDiariaRepository,
                                   CurrentOrganizadorPort currentOrganizador) {
        this.capacidadDiariaRepository = capacidadDiariaRepository;
        this.currentOrganizador = currentOrganizador;
    }

    @Override
    public CapacidadDiaria execute(BigDecimal limiteHoras) {
        var organizadorId = currentOrganizador.currentOrganizadorId();

        var existente = capacidadDiariaRepository.findCurrentByOrganizadorId(organizadorId);
        if (existente.isPresent()) {
            CapacidadDiaria capacidad = existente.get();
            capacidad.actualizarLimite(limiteHoras, LocalDate.now());
            return capacidadDiariaRepository.save(capacidad);
        }

        return capacidadDiariaRepository.save(new CapacidadDiaria(organizadorId, LocalDate.now(), limiteHoras));
    }
}

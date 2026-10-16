package com.events.application.usecase;

import com.events.application.port.in.ChangeSubtareaStatusPort;
import com.events.application.port.out.CapacidadDiariaRepositoryPort;
import com.events.application.port.out.CurrentOrganizadorPort;
import com.events.application.port.out.SubtareaRepositoryPort;
import com.events.domain.entity.EstadoSubtarea;
import com.events.domain.entity.Subtarea;
import com.events.domain.exception.SubtareaNotFoundException;
import java.util.UUID;

public class ChangeSubtareaStatusUseCase implements ChangeSubtareaStatusPort {

    private final SubtareaRepositoryPort subtareaRepository;
    private final CapacidadDiariaValidator capacidadDiaria;
    private final CurrentOrganizadorPort currentOrganizador;

    public ChangeSubtareaStatusUseCase(SubtareaRepositoryPort subtareaRepository,
                                       CapacidadDiariaRepositoryPort capacidadDiariaRepository,
                                       CurrentOrganizadorPort currentOrganizador) {
        this.subtareaRepository = subtareaRepository;
        this.capacidadDiaria = new CapacidadDiariaValidator(capacidadDiariaRepository, subtareaRepository);
        this.currentOrganizador = currentOrganizador;
    }

    @Override
    public Subtarea execute(UUID subtareaId, EstadoSubtarea estado, String nota) {
        Subtarea subtarea = subtareaRepository.findByIdAndOrganizadorId(subtareaId, currentOrganizador.currentOrganizadorId())
                .orElseThrow(() -> new SubtareaNotFoundException("No encontramos la subtarea solicitada."));

        // Las subtareas DONE no cuentan en la carga diaria: al sacarlas de DONE vuelven a sumar.
        boolean vuelveACargar = subtarea.getEstado() == EstadoSubtarea.DONE && estado != EstadoSubtarea.DONE;
        if (vuelveACargar) {
            capacidadDiaria.validar(subtarea.getEvento().getOrganizador().getId(), subtarea.getFechaObjetivo(),
                    subtarea.getHorasEstimadas(), subtarea.getId());
        }

        switch (estado) {
            case DONE -> subtarea.marcarHecha();
            case POSTPONED -> subtarea.posponer(nota);
            case PENDING -> subtarea.reabrir();
        }

        return subtareaRepository.save(subtarea);
    }
}

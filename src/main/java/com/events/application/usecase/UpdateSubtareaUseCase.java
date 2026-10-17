package com.events.application.usecase;

import com.events.application.port.in.UpdateSubtareaPort;
import com.events.application.port.out.CurrentOrganizadorPort;
import com.events.application.port.out.CapacidadDiariaRepositoryPort;
import com.events.application.port.out.SubtareaRepositoryPort;
import com.events.domain.entity.EstadoSubtarea;
import com.events.domain.entity.Subtarea;
import com.events.domain.exception.SubtareaNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class UpdateSubtareaUseCase implements UpdateSubtareaPort {

    private final SubtareaRepositoryPort subtareaRepository;
    private final CapacidadDiariaValidator capacidadDiaria;
    private final CurrentOrganizadorPort currentOrganizador;

    public UpdateSubtareaUseCase(SubtareaRepositoryPort subtareaRepository,
                                  CapacidadDiariaRepositoryPort capacidadDiariaRepository,
                                  CurrentOrganizadorPort currentOrganizador) {
        this.subtareaRepository = subtareaRepository;
        this.capacidadDiaria = new CapacidadDiariaValidator(capacidadDiariaRepository, subtareaRepository);
        this.currentOrganizador = currentOrganizador;
    }

    @Override
    public Subtarea execute(UUID subtareaId, String nombre, String descripcion, LocalDate fechaObjetivo, BigDecimal horasEstimadas) {
        Subtarea subtarea = subtareaRepository.findByIdAndOrganizadorId(subtareaId, currentOrganizador.currentOrganizadorId())
                .orElseThrow(() -> new SubtareaNotFoundException("No encontramos la subtarea solicitada."));

        // Una subtarea DONE no suma a la carga diaria, asi que moverla no puede sobrecargar el dia.
        boolean cambiaCarga = fechaObjetivo != null || horasEstimadas != null;
        if (cambiaCarga && subtarea.getEstado() != EstadoSubtarea.DONE) {
            LocalDate fechaEfectiva = fechaObjetivo != null ? fechaObjetivo : subtarea.getFechaObjetivo();
            BigDecimal horasEfectivas = horasEstimadas != null ? horasEstimadas : subtarea.getHorasEstimadas();
            capacidadDiaria.validar(subtarea.getEvento().getOrganizador().getId(), fechaEfectiva, horasEfectivas, subtarea.getId());
        }

        subtarea.actualizar(nombre, descripcion, fechaObjetivo, horasEstimadas);
        return subtareaRepository.save(subtarea);
    }
}

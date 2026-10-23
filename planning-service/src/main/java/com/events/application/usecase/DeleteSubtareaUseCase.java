package com.events.application.usecase;

import com.events.application.port.in.DeleteSubtareaPort;
import com.events.application.port.out.CurrentOrganizadorPort;
import com.events.application.port.out.SubtareaRepositoryPort;
import com.events.domain.exception.SubtareaNotFoundException;
import java.util.UUID;

public class DeleteSubtareaUseCase implements DeleteSubtareaPort {

    private final SubtareaRepositoryPort subtareaRepository;
    private final CurrentOrganizadorPort currentOrganizador;

    public DeleteSubtareaUseCase(SubtareaRepositoryPort subtareaRepository,
                                 CurrentOrganizadorPort currentOrganizador) {
        this.subtareaRepository = subtareaRepository;
        this.currentOrganizador = currentOrganizador;
    }

    @Override
    public void execute(UUID subtareaId) {
        subtareaRepository.findByIdAndOrganizadorId(subtareaId, currentOrganizador.currentOrganizadorId())
                .orElseThrow(() -> new SubtareaNotFoundException("No encontramos la subtarea solicitada."));
        subtareaRepository.deleteById(subtareaId);
    }
}

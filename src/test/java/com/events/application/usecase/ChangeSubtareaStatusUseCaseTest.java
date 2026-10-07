package com.events.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.events.application.port.out.CurrentOrganizadorPort;
import com.events.application.port.out.SubtareaRepositoryPort;
import com.events.domain.entity.EstadoSubtarea;
import com.events.domain.entity.Subtarea;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChangeSubtareaStatusUseCaseTest {

    private static final UUID ORGANIZADOR_ID = UUID.randomUUID();
    private final CurrentOrganizadorPort currentOrganizador = mock(CurrentOrganizadorPort.class);

    {
        when(currentOrganizador.currentOrganizadorId()).thenReturn(ORGANIZADOR_ID);
    }

    private final SubtareaRepositoryPort subtareaRepository = mock(SubtareaRepositoryPort.class);
    private final ChangeSubtareaStatusUseCase useCase = new ChangeSubtareaStatusUseCase(subtareaRepository, currentOrganizador);

    private Subtarea nuevaSubtarea() {
        return new Subtarea("Confirmar catering", LocalDate.now(), BigDecimal.ONE);
    }

    @Test
    void marcaComoHecha() {
        UUID id = UUID.randomUUID();
        when(subtareaRepository.findByIdAndOrganizadorId(id, ORGANIZADOR_ID)).thenReturn(Optional.of(nuevaSubtarea()));
        when(subtareaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var actualizada = useCase.execute(id, EstadoSubtarea.DONE, null);

        assertThat(actualizada.getEstado()).isEqualTo(EstadoSubtarea.DONE);
        assertThat(actualizada.getDoneAt()).isNotNull();
    }

    @Test
    void posponeConNotaOpcional() {
        UUID id = UUID.randomUUID();
        when(subtareaRepository.findByIdAndOrganizadorId(id, ORGANIZADOR_ID)).thenReturn(Optional.of(nuevaSubtarea()));
        when(subtareaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var actualizada = useCase.execute(id, EstadoSubtarea.POSTPONED, "Esperando confirmacion de salon");

        assertThat(actualizada.getEstado()).isEqualTo(EstadoSubtarea.POSTPONED);
        assertThat(actualizada.getNota()).isEqualTo("Esperando confirmacion de salon");
    }
}

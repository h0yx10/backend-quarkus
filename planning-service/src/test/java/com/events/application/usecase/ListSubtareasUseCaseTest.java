package com.events.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.events.application.port.out.CurrentOrganizadorPort;
import com.events.application.port.out.EventoRepositoryPort;
import com.events.application.port.out.SubtareaRepositoryPort;
import com.events.domain.entity.Subtarea;
import com.events.domain.exception.EventoNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListSubtareasUseCaseTest {

    private static final UUID ORGANIZADOR_ID = UUID.randomUUID();
    private final CurrentOrganizadorPort currentOrganizador = mock(CurrentOrganizadorPort.class);

    {
        when(currentOrganizador.currentOrganizadorId()).thenReturn(ORGANIZADOR_ID);
    }

    private final EventoRepositoryPort eventoRepository = mock(EventoRepositoryPort.class);
    private final SubtareaRepositoryPort subtareaRepository = mock(SubtareaRepositoryPort.class);
    private final ListSubtareasUseCase useCase = new ListSubtareasUseCase(eventoRepository, subtareaRepository, currentOrganizador);

    @Test
    void listaLasSubtareasDeUnEventoExistente() {
        UUID eventoId = UUID.randomUUID();
        when(eventoRepository.existsByIdAndOrganizadorId(eventoId, ORGANIZADOR_ID)).thenReturn(true);
        Subtarea subtarea = new Subtarea("Reservar salon", LocalDate.now(), BigDecimal.ONE);
        when(subtareaRepository.findByEventoId(eventoId)).thenReturn(List.of(subtarea));

        assertThat(useCase.execute(eventoId)).containsExactly(subtarea);
    }

    @Test
    void fallaSiElEventoNoExiste() {
        UUID eventoId = UUID.randomUUID();
        when(eventoRepository.existsByIdAndOrganizadorId(eventoId, ORGANIZADOR_ID)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(eventoId)).isInstanceOf(EventoNotFoundException.class);
    }
}

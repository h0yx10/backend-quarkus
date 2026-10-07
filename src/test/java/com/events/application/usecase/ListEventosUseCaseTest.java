package com.events.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.events.application.port.out.CurrentOrganizadorPort;
import com.events.application.port.out.EventoRepositoryPort;
import com.events.domain.entity.Evento;
import com.events.domain.entity.Organizador;
import com.events.domain.entity.Usuario;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListEventosUseCaseTest {

    private final EventoRepositoryPort eventoRepository = mock(EventoRepositoryPort.class);
    private final CurrentOrganizadorPort currentOrganizador = mock(CurrentOrganizadorPort.class);
    private final ListEventosUseCase useCase = new ListEventosUseCase(eventoRepository, currentOrganizador);

    @Test
    void listaLosEventosDelOrganizadorActual() {
        UUID organizadorId = UUID.randomUUID();
        when(currentOrganizador.currentOrganizadorId()).thenReturn(organizadorId);
        Evento evento = new Evento("Boda", "Social", null, null, null, null, null, new Organizador(new Usuario("Demo", "demo@x.com", "hash")));
        when(eventoRepository.findByOrganizadorId(organizadorId)).thenReturn(List.of(evento));

        assertThat(useCase.execute()).containsExactly(evento);
    }
}

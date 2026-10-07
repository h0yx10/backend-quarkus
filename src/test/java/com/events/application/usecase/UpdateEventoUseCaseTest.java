package com.events.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.events.application.port.out.CurrentOrganizadorPort;
import com.events.application.port.out.EventoRepositoryPort;
import com.events.domain.entity.Evento;
import com.events.domain.entity.Organizador;
import com.events.domain.entity.Usuario;
import com.events.domain.exception.EventoNotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UpdateEventoUseCaseTest {

    private static final UUID ORGANIZADOR_ID = UUID.randomUUID();
    private final CurrentOrganizadorPort currentOrganizador = mock(CurrentOrganizadorPort.class);

    {
        when(currentOrganizador.currentOrganizadorId()).thenReturn(ORGANIZADOR_ID);
    }

    private final EventoRepositoryPort eventoRepository = mock(EventoRepositoryPort.class);
    private final UpdateEventoUseCase useCase = new UpdateEventoUseCase(eventoRepository, currentOrganizador);

    @Test
    void actualizaSoloLosCamposEnviados() {
        UUID eventoId = UUID.randomUUID();
        Evento evento = new Evento("Boda", "Social", "Cliente original", null, null, "Salon", null, new Organizador(new Usuario("Demo", "demo@x.com", "hash")));
        when(eventoRepository.findByIdAndOrganizadorId(eventoId, ORGANIZADOR_ID)).thenReturn(Optional.of(evento));
        when(eventoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var actualizado = useCase.execute(eventoId, "Boda actualizada", null, null, null, null, null, null);

        assertThat(actualizado.getNombre()).isEqualTo("Boda actualizada");
        assertThat(actualizado.getCliente()).isEqualTo("Cliente original");
    }

    @Test
    void fallaSiElEventoNoExiste() {
        UUID eventoId = UUID.randomUUID();
        when(eventoRepository.findByIdAndOrganizadorId(eventoId, ORGANIZADOR_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(eventoId, "Nuevo", null, null, null, null, null, null))
                .isInstanceOf(EventoNotFoundException.class);
    }
}

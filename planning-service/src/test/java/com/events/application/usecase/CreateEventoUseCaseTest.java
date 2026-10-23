package com.events.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.events.application.port.in.NuevaSubtareaData;
import com.events.application.port.out.CurrentOrganizadorPort;
import com.events.application.port.out.CapacidadDiariaRepositoryPort;
import com.events.application.port.out.EventoRepositoryPort;
import com.events.application.port.out.SubtareaRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CreateEventoUseCaseTest {

    private final EventoRepositoryPort eventoRepository = mock(EventoRepositoryPort.class);
    private final CapacidadDiariaRepositoryPort capacidadDiariaRepository = mock(CapacidadDiariaRepositoryPort.class);
    private final SubtareaRepositoryPort subtareaRepository = mock(SubtareaRepositoryPort.class);
    private final CurrentOrganizadorPort currentOrganizador = mock(CurrentOrganizadorPort.class);
    private final CreateEventoUseCase useCase = new CreateEventoUseCase(eventoRepository,
            capacidadDiariaRepository, subtareaRepository, currentOrganizador);

    @Test
    void creaElEventoConSusSubtareasIniciales() {
        UUID organizadorId = UUID.randomUUID();
        when(currentOrganizador.currentOrganizadorId()).thenReturn(organizadorId);
        when(capacidadDiariaRepository.findCurrentByOrganizadorId(any())).thenReturn(Optional.empty());
        when(subtareaRepository.sumHorasPlanificadas(any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(eventoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var subtareas = List.of(new NuevaSubtareaData("Reservar salon", "Confirmar aforo", LocalDate.now().plusDays(1), BigDecimal.valueOf(2)));
        var evento = useCase.execute("Boda", "Social", "Cliente", null, LocalDateTime.now().plusDays(10), "Salon", null, subtareas);

        assertThat(evento.getNombre()).isEqualTo("Boda");
        assertThat(evento.getSubtareas()).hasSize(1);
        assertThat(evento.getSubtareas().get(0).getDescripcion()).isEqualTo("Confirmar aforo");
        assertThat(evento.getSubtareas().get(0).getNombre()).isEqualTo("Reservar salon");
    }

    @Test
    void asociaElEventoAlOrganizadorAutenticado() {
        UUID organizadorId = UUID.randomUUID();
        when(currentOrganizador.currentOrganizadorId()).thenReturn(organizadorId);
        when(eventoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var evento = useCase.execute("Boda", "Social", null, null, LocalDateTime.now(), null, null, List.of());

        assertThat(evento.getOrganizadorId()).isEqualTo(organizadorId);
    }
}

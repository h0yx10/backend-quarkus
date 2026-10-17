package com.events.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.events.application.port.out.CurrentOrganizadorPort;
import com.events.application.port.out.CapacidadDiariaRepositoryPort;
import com.events.application.port.out.EventoRepositoryPort;
import com.events.application.port.out.SubtareaRepositoryPort;
import com.events.domain.entity.CapacidadDiaria;
import com.events.domain.entity.Evento;
import com.events.domain.entity.Organizador;
import com.events.domain.entity.Usuario;
import com.events.domain.exception.EventoNotFoundException;
import com.events.domain.exception.CapacityConflictException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CreateSubtareaUseCaseTest {

    private static final UUID ORGANIZADOR_ID = UUID.randomUUID();
    private final CurrentOrganizadorPort currentOrganizador = mock(CurrentOrganizadorPort.class);

    {
        when(currentOrganizador.currentOrganizadorId()).thenReturn(ORGANIZADOR_ID);
    }

    private final EventoRepositoryPort eventoRepository = mock(EventoRepositoryPort.class);
    private final SubtareaRepositoryPort subtareaRepository = mock(SubtareaRepositoryPort.class);
    private final CapacidadDiariaRepositoryPort capacidadDiariaRepository = mock(CapacidadDiariaRepositoryPort.class);
    private final CreateSubtareaUseCase useCase = new CreateSubtareaUseCase(eventoRepository, subtareaRepository,
            capacidadDiariaRepository, currentOrganizador);

    @Test
    void creaLaSubtareaAsociadaAlEvento() {
        UUID eventoId = UUID.randomUUID();
        Evento evento = new Evento("Boda", "Social", null, null, null, null, null, new Organizador(new Usuario("Demo", "demo@x.com", "hash")));
        when(eventoRepository.findByIdAndOrganizadorId(eventoId, ORGANIZADOR_ID)).thenReturn(Optional.of(evento));
        when(capacidadDiariaRepository.findCurrentByOrganizadorId(any())).thenReturn(Optional.empty());
        when(subtareaRepository.sumHorasPlanificadas(any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(subtareaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var subtarea = useCase.execute(eventoId, "Enviar invitaciones", LocalDate.now().plusDays(1), BigDecimal.valueOf(3));

        assertThat(subtarea.getNombre()).isEqualTo("Enviar invitaciones");
        assertThat(subtarea.getEvento()).isSameAs(evento);
    }

    @Test
    void fallaSiElEventoNoExiste() {
        UUID eventoId = UUID.randomUUID();
        when(eventoRepository.findByIdAndOrganizadorId(eventoId, ORGANIZADOR_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(eventoId, "Tarea", LocalDate.now(), BigDecimal.ONE))
                .isInstanceOf(EventoNotFoundException.class);
    }
    @Test
    void transmiteDescripcionAlRepositorio() {
        UUID eventoId = UUID.randomUUID();
        Evento evento = new Evento("Boda", "Social", null, null, null, null, null,
                new Organizador(new Usuario("Camila", "camila@correo.com", "hash")));
        when(eventoRepository.findByIdAndOrganizadorId(eventoId, ORGANIZADOR_ID)).thenReturn(Optional.of(evento));
        when(capacidadDiariaRepository.findCurrentByOrganizadorId(any())).thenReturn(Optional.empty());
        when(subtareaRepository.sumHorasPlanificadas(any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(subtareaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        var subtarea = useCase.execute(eventoId, "Catering", "Confirmar menu", LocalDate.now(), BigDecimal.ONE);
        assertThat(subtarea.getDescripcion()).isEqualTo("Confirmar menu");
    }

    @Test
    void rechazaLaCreacionCuandoSuperaElLimiteDiario() {
        UUID eventoId = UUID.randomUUID();
        Evento evento = new Evento("Boda", "Social", null, null, null, null, null,
                new Organizador(new Usuario("Demo", "demo@x.com", "hash")));
        LocalDate fecha = LocalDate.now();
        when(eventoRepository.findByIdAndOrganizadorId(eventoId, ORGANIZADOR_ID)).thenReturn(Optional.of(evento));
        when(capacidadDiariaRepository.findCurrentByOrganizadorId(any()))
                .thenReturn(Optional.of(new CapacidadDiaria(evento.getOrganizador(), fecha, BigDecimal.valueOf(6))));
        when(subtareaRepository.sumHorasPlanificadas(any(), any(), any())).thenReturn(BigDecimal.valueOf(5));

        assertThatThrownBy(() -> useCase.execute(eventoId, "Catering", fecha, BigDecimal.valueOf(2)))
                .isInstanceOf(CapacityConflictException.class)
                .satisfies(ex -> {
                    CapacityConflictException conflict = (CapacityConflictException) ex;
                    assertThat(conflict.getPlannedHours()).isEqualByComparingTo("7");
                    assertThat(conflict.getLimitHours()).isEqualByComparingTo("6");
                    assertThat(conflict.getExceedsBy()).isEqualByComparingTo("1");
                });
        verify(subtareaRepository, never()).save(any());
    }
}

package com.events.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.events.application.port.out.CapacidadDiariaRepositoryPort;
import com.events.application.port.out.CurrentOrganizadorPort;
import com.events.application.port.out.SubtareaRepositoryPort;
import com.events.domain.entity.CapacidadDiaria;
import com.events.domain.entity.EstadoSubtarea;
import com.events.domain.entity.Evento;
import com.events.domain.entity.Organizador;
import com.events.domain.entity.Subtarea;
import com.events.domain.entity.Usuario;
import com.events.domain.exception.CapacityConflictException;
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
    private final CapacidadDiariaRepositoryPort capacidadDiariaRepository = mock(CapacidadDiariaRepositoryPort.class);
    private final ChangeSubtareaStatusUseCase useCase =
            new ChangeSubtareaStatusUseCase(subtareaRepository, capacidadDiariaRepository, currentOrganizador);

    private Subtarea nuevaSubtarea() {
        return new Subtarea("Confirmar catering", LocalDate.now(), BigDecimal.ONE);
    }

    private Subtarea subtareaHechaConEvento(BigDecimal horas) {
        Organizador organizador = new Organizador(new Usuario("Demo", "demo@x.com", "hash"));
        Evento evento = new Evento("Evento", "Social", "Cliente", null, null, null, null, organizador);
        Subtarea subtarea = new Subtarea("Reservar salon", LocalDate.now().plusDays(1), horas);
        subtarea.asociarEvento(evento);
        subtarea.marcarHecha();
        return subtarea;
    }

    private void limiteDeSeisHoras() {
        when(capacidadDiariaRepository.findCurrentByOrganizadorId(any()))
                .thenReturn(Optional.of(new CapacidadDiaria(new Organizador(new Usuario("Demo", "demo@x.com", "hash")), LocalDate.now(), BigDecimal.valueOf(6))));
    }

    @Test
    void marcaComoHecha() {
        UUID id = UUID.randomUUID();
        when(subtareaRepository.findByIdAndOrganizadorId(id, ORGANIZADOR_ID)).thenReturn(Optional.of(nuevaSubtarea()));
        when(subtareaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var actualizada = useCase.execute(id, EstadoSubtarea.DONE, null);

        assertThat(actualizada.getEstado()).isEqualTo(EstadoSubtarea.DONE);
        assertThat(actualizada.getDoneAt()).isNotNull();
        verify(subtareaRepository, never()).sumHorasPlanificadas(any(), any(), any());
    }

    @Test
    void registraLaNotaAlMarcarComoHecha() {
        UUID id = UUID.randomUUID();
        when(subtareaRepository.findByIdAndOrganizadorId(id, ORGANIZADOR_ID)).thenReturn(Optional.of(nuevaSubtarea()));
        when(subtareaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var actualizada = useCase.execute(id, EstadoSubtarea.DONE, "Catering confirmado");

        assertThat(actualizada.getEstado()).isEqualTo(EstadoSubtarea.DONE);
        assertThat(actualizada.getNota()).isEqualTo("Catering confirmado");
    }

    @Test
    void posponeConNotaOpcional() {
        UUID id = UUID.randomUUID();
        when(subtareaRepository.findByIdAndOrganizadorId(id, ORGANIZADOR_ID)).thenReturn(Optional.of(nuevaSubtarea()));
        when(subtareaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var actualizada = useCase.execute(id, EstadoSubtarea.POSTPONED, "Esperando confirmacion de salon");

        assertThat(actualizada.getEstado()).isEqualTo(EstadoSubtarea.POSTPONED);
        assertThat(actualizada.getNota()).isEqualTo("Esperando confirmacion de salon");
        verify(subtareaRepository, never()).sumHorasPlanificadas(any(), any(), any());
    }

    @Test
    void rechazaReabrirSubtareaHechaSiSuperaElLimite() {
        Subtarea subtarea = subtareaHechaConEvento(BigDecimal.valueOf(4));
        when(subtareaRepository.findByIdAndOrganizadorId(any(), any())).thenReturn(Optional.of(subtarea));
        limiteDeSeisHoras();
        // 6h de otras subtareas ese dia + 4h de la reabierta = 10h > 6h limite
        when(subtareaRepository.sumHorasPlanificadas(any(), any(), any())).thenReturn(BigDecimal.valueOf(6));

        assertThatThrownBy(() -> useCase.execute(null, EstadoSubtarea.PENDING, null))
                .isInstanceOf(CapacityConflictException.class)
                .satisfies(ex -> {
                    CapacityConflictException conflict = (CapacityConflictException) ex;
                    assertThat(conflict.getPlannedHours()).isEqualByComparingTo("10");
                    assertThat(conflict.getLimitHours()).isEqualByComparingTo("6");
                    assertThat(conflict.getExceedsBy()).isEqualByComparingTo("4");
                });

        verify(subtareaRepository, never()).save(any());
        assertThat(subtarea.getEstado()).isEqualTo(EstadoSubtarea.DONE);
    }

    @Test
    void rechazaPosponerSubtareaHechaSiSuperaElLimite() {
        Subtarea subtarea = subtareaHechaConEvento(BigDecimal.valueOf(2));
        when(subtareaRepository.findByIdAndOrganizadorId(any(), any())).thenReturn(Optional.of(subtarea));
        limiteDeSeisHoras();
        when(subtareaRepository.sumHorasPlanificadas(any(), any(), any())).thenReturn(BigDecimal.valueOf(5));

        assertThatThrownBy(() -> useCase.execute(null, EstadoSubtarea.POSTPONED, "Sin proveedor"))
                .isInstanceOf(CapacityConflictException.class);

        verify(subtareaRepository, never()).save(any());
        assertThat(subtarea.getEstado()).isEqualTo(EstadoSubtarea.DONE);
    }

    @Test
    void permiteReabrirCuandoElTotalEsExactamenteElLimite() {
        Subtarea subtarea = subtareaHechaConEvento(BigDecimal.valueOf(2));
        when(subtareaRepository.findByIdAndOrganizadorId(any(), any())).thenReturn(Optional.of(subtarea));
        limiteDeSeisHoras();
        when(subtareaRepository.sumHorasPlanificadas(any(), any(), any())).thenReturn(BigDecimal.valueOf(4));
        when(subtareaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var actualizada = useCase.execute(null, EstadoSubtarea.PENDING, null);

        assertThat(actualizada.getEstado()).isEqualTo(EstadoSubtarea.PENDING);
        verify(subtareaRepository).save(subtarea);
    }
}

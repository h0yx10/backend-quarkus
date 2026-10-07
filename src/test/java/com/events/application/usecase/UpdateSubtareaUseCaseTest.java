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
import com.events.application.port.out.SubtareaRepositoryPort;
import com.events.domain.entity.CapacidadDiaria;
import com.events.domain.entity.Evento;
import com.events.domain.entity.Organizador;
import com.events.domain.entity.Usuario;
import com.events.domain.entity.Subtarea;
import com.events.domain.exception.CapacityConflictException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UpdateSubtareaUseCaseTest {

    private static final UUID ORGANIZADOR_ID = UUID.randomUUID();
    private final CurrentOrganizadorPort currentOrganizador = mock(CurrentOrganizadorPort.class);

    {
        when(currentOrganizador.currentOrganizadorId()).thenReturn(ORGANIZADOR_ID);
    }

    private final SubtareaRepositoryPort subtareaRepository = mock(SubtareaRepositoryPort.class);
    private final CapacidadDiariaRepositoryPort capacidadDiariaRepository = mock(CapacidadDiariaRepositoryPort.class);
    private final UpdateSubtareaUseCase useCase = new UpdateSubtareaUseCase(subtareaRepository, capacidadDiariaRepository, currentOrganizador);

    private Subtarea subtareaConEvento() {
        Organizador organizador = new Organizador(new Usuario("Demo", "demo@x.com", "hash"));
        Evento evento = new Evento("Evento", "Social", "Cliente", null, null, null, null, organizador);
        Subtarea subtarea = new Subtarea("Reservar salon", LocalDate.now().plusDays(1), BigDecimal.valueOf(2));
        subtarea.asociarEvento(evento);
        return subtarea;
    }

    @Test
    void reprogramaCuandoNoHaySobrecarga() {
        Subtarea subtarea = subtareaConEvento();
        when(subtareaRepository.findByIdAndOrganizadorId(any(), any())).thenReturn(Optional.of(subtarea));
        when(capacidadDiariaRepository.findCurrentByOrganizadorId(any())).thenReturn(Optional.empty());
        when(subtareaRepository.sumHorasPlanificadas(any(), any(), any())).thenReturn(BigDecimal.valueOf(2));
        when(subtareaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDate nuevaFecha = LocalDate.now().plusDays(2);
        Subtarea actualizada = useCase.execute(null, null, nuevaFecha, BigDecimal.valueOf(2));

        assertThat(actualizada.getFechaObjetivo()).isEqualTo(nuevaFecha);
        verify(subtareaRepository).save(subtarea);
    }

    @Test
    void detectaConflictoYNoGuardaCuandoSuperaElLimite() {
        Subtarea subtarea = subtareaConEvento();
        when(subtareaRepository.findByIdAndOrganizadorId(any(), any())).thenReturn(Optional.of(subtarea));
        when(capacidadDiariaRepository.findCurrentByOrganizadorId(any()))
                .thenReturn(Optional.of(new CapacidadDiaria(new Organizador(new Usuario("Demo", "demo@x.com", "hash")), LocalDate.now(), BigDecimal.valueOf(6))));
        // 5h ya planificadas ese dia (sin contar la propia subtarea) + 2h nuevas = 7h > 6h limite
        when(subtareaRepository.sumHorasPlanificadas(any(), any(), any())).thenReturn(BigDecimal.valueOf(5));

        LocalDate nuevaFecha = LocalDate.now().plusDays(1);
        assertThatThrownBy(() -> useCase.execute(null, null, "No guardar", nuevaFecha, BigDecimal.valueOf(2)))
                .isInstanceOf(CapacityConflictException.class)
                .satisfies(ex -> {
                    CapacityConflictException conflict = (CapacityConflictException) ex;
                    assertThat(conflict.getPlannedHours()).isEqualByComparingTo("7");
                    assertThat(conflict.getLimitHours()).isEqualByComparingTo("6");
                    assertThat(conflict.getExceedsBy()).isEqualByComparingTo("1");
                });

        verify(subtareaRepository, never()).save(any());
        assertThat(subtarea.getDescripcion()).isNull();
    }

    @Test
    void permiteGuardarCuandoElTotalEsExactamenteElLimite() {
        Subtarea subtarea = subtareaConEvento();
        when(subtareaRepository.findByIdAndOrganizadorId(any(), any())).thenReturn(Optional.of(subtarea));
        when(capacidadDiariaRepository.findCurrentByOrganizadorId(any()))
                .thenReturn(Optional.of(new CapacidadDiaria(new Organizador(new Usuario("Demo", "demo@x.com", "hash")), LocalDate.now(), BigDecimal.valueOf(6))));
        when(subtareaRepository.sumHorasPlanificadas(any(), any(), any())).thenReturn(BigDecimal.valueOf(4));
        when(subtareaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(null, null, LocalDate.now().plusDays(1), BigDecimal.valueOf(2));

        verify(subtareaRepository).save(subtarea);
    }

    @Test
    void noValidaCargaCuandoSoloCambiaElNombre() {
        Subtarea subtarea = subtareaConEvento();
        when(subtareaRepository.findByIdAndOrganizadorId(any(), any())).thenReturn(Optional.of(subtarea));
        when(subtareaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(null, "Nuevo nombre", null, null);

        verify(capacidadDiariaRepository, never()).findCurrentByOrganizadorId(any());
        verify(subtareaRepository).save(subtarea);
    }
    @Test
    void editaSoloDescripcionSinRecalcularCarga() {
        Subtarea subtarea = subtareaConEvento();
        var fecha = subtarea.getFechaObjetivo();
        when(subtareaRepository.findByIdAndOrganizadorId(any(), any())).thenReturn(Optional.of(subtarea));
        when(subtareaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        var resultado = useCase.execute(null, null, "Menu actualizado", null, null);
        assertThat(resultado.getDescripcion()).isEqualTo("Menu actualizado");
        assertThat(resultado.getFechaObjetivo()).isEqualTo(fecha);
        assertThat(resultado.getHorasEstimadas()).isEqualByComparingTo("2");
        verify(capacidadDiariaRepository, never()).findCurrentByOrganizadorId(any());
        verify(subtareaRepository, never()).sumHorasPlanificadas(any(), any(), any());
    }
    @Test
    void conservaDescripcionSiNoSeEnviaYPermiteVaciarla() {
        Subtarea subtarea = subtareaConEvento();
        subtarea.actualizar(null, "Anterior", null, null);
        when(subtareaRepository.findByIdAndOrganizadorId(any(), any())).thenReturn(Optional.of(subtarea));
        when(subtareaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        useCase.execute(null, "Otro nombre", null, null);
        assertThat(subtarea.getDescripcion()).isEqualTo("Anterior");
        useCase.execute(null, null, "", null, null);
        assertThat(subtarea.getDescripcion()).isEmpty();
    }
    @Test
    void rechazaDescripcionDemasiadoLargaSinGuardarNiModificar() {
        Subtarea subtarea = subtareaConEvento();
        when(subtareaRepository.findByIdAndOrganizadorId(any(), any())).thenReturn(Optional.of(subtarea));
        assertThatThrownBy(() -> useCase.execute(null, "Otro nombre", "a".repeat(256), null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(subtarea.getNombre()).isEqualTo("Reservar salon");
        assertThat(subtarea.getDescripcion()).isNull();
        verify(subtareaRepository, never()).save(any());
    }
}

package com.events.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.events.application.port.in.OverloadCheckResult;
import com.events.application.port.out.CurrentOrganizadorPort;
import com.events.application.port.out.CapacidadDiariaRepositoryPort;
import com.events.application.port.out.SubtareaRepositoryPort;
import com.events.domain.entity.CapacidadDiaria;
import com.events.domain.entity.Evento;
import com.events.domain.entity.Organizador;
import com.events.domain.entity.Usuario;
import com.events.domain.entity.Subtarea;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CheckOverloadConflictUseCaseTest {

    private static final UUID ORGANIZADOR_ID = UUID.randomUUID();
    private final CurrentOrganizadorPort currentOrganizador = mock(CurrentOrganizadorPort.class);

    {
        when(currentOrganizador.currentOrganizadorId()).thenReturn(ORGANIZADOR_ID);
    }

    private final SubtareaRepositoryPort subtareaRepository = mock(SubtareaRepositoryPort.class);
    private final CapacidadDiariaRepositoryPort capacidadDiariaRepository = mock(CapacidadDiariaRepositoryPort.class);
    private final CheckOverloadConflictUseCase useCase =
            new CheckOverloadConflictUseCase(subtareaRepository, capacidadDiariaRepository, currentOrganizador);

    private Subtarea subtareaConEvento() {
        Organizador organizador = new Organizador(new Usuario("Demo", "demo@x.com", "hash"));
        Evento evento = new Evento("Evento", "Social", "Cliente", null, null, null, null, organizador);
        Subtarea subtarea = new Subtarea("Reservar salon", LocalDate.now().plusDays(1), BigDecimal.valueOf(2));
        subtarea.asociarEvento(evento);
        return subtarea;
    }

    @Test
    void noPersisteYReportaConflictoCuandoSeSuperaElLimite() {
        Subtarea subtarea = subtareaConEvento();
        when(subtareaRepository.findByIdAndOrganizadorId(any(), any())).thenReturn(Optional.of(subtarea));
        when(capacidadDiariaRepository.findCurrentByOrganizadorId(any()))
                .thenReturn(Optional.of(new CapacidadDiaria(new Organizador(new Usuario("Demo", "demo@x.com", "hash")), LocalDate.now(), BigDecimal.valueOf(6))));
        when(subtareaRepository.sumHorasPlanificadas(any(), any(), any())).thenReturn(BigDecimal.valueOf(5));

        OverloadCheckResult result = useCase.execute(null, LocalDate.now().plusDays(1), BigDecimal.valueOf(2));

        assertThat(result.conflict()).isTrue();
        assertThat(result.plannedHours()).isEqualByComparingTo("7");
        assertThat(result.exceedsBy()).isEqualByComparingTo("1");
    }

    @Test
    void usaElLimitePorDefectoCuandoNoHayCapacidadConfigurada() {
        Subtarea subtarea = subtareaConEvento();
        when(subtareaRepository.findByIdAndOrganizadorId(any(), any())).thenReturn(Optional.of(subtarea));
        when(capacidadDiariaRepository.findCurrentByOrganizadorId(any())).thenReturn(Optional.empty());
        when(subtareaRepository.sumHorasPlanificadas(any(), any(), any())).thenReturn(BigDecimal.ZERO);

        OverloadCheckResult result = useCase.execute(null, LocalDate.now().plusDays(1), BigDecimal.valueOf(2));

        assertThat(result.conflict()).isFalse();
        assertThat(result.limitHours()).isEqualByComparingTo(CapacidadDiaria.LIMITE_POR_DEFECTO);
    }
}

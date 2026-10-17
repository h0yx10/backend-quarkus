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
import com.events.application.port.out.OrganizadorRepositoryPort;
import com.events.application.port.out.SubtareaRepositoryPort;
import com.events.domain.entity.CapacidadDiaria;
import com.events.domain.entity.Organizador;
import com.events.domain.entity.Usuario;
import com.events.domain.exception.LimiteBajoCargaPlanificadaException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdateCapacidadUseCaseTest {

    private final CapacidadDiariaRepositoryPort capacidadDiariaRepository = mock(CapacidadDiariaRepositoryPort.class);
    private final OrganizadorRepositoryPort organizadorRepository = mock(OrganizadorRepositoryPort.class);
    private final SubtareaRepositoryPort subtareaRepository = mock(SubtareaRepositoryPort.class);
    private final CurrentOrganizadorPort currentOrganizador = mock(CurrentOrganizadorPort.class);
    private final UpdateCapacidadUseCase useCase =
            new UpdateCapacidadUseCase(capacidadDiariaRepository, organizadorRepository, subtareaRepository, currentOrganizador);
    private final UUID organizadorId = UUID.randomUUID();

    @BeforeEach
    void organizadorActual() {
        when(currentOrganizador.currentOrganizadorId()).thenReturn(organizadorId);
        when(subtareaRepository.horasPlanificadasPorDiaDesde(any(), any())).thenReturn(new TreeMap<>());
        when(capacidadDiariaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void capacidadExistente(int horas) {
        CapacidadDiaria existente = new CapacidadDiaria(new Organizador(new Usuario("Demo", "demo@x.com", "hash")), LocalDate.now().minusDays(1), BigDecimal.valueOf(horas));
        when(capacidadDiariaRepository.findCurrentByOrganizadorId(organizadorId)).thenReturn(Optional.of(existente));
    }

    @Test
    void creaUnaCapacidadNuevaCuandoNoExisteUnaPrevia() {
        when(capacidadDiariaRepository.findCurrentByOrganizadorId(organizadorId)).thenReturn(Optional.empty());
        when(organizadorRepository.findById(organizadorId)).thenReturn(Optional.of(new Organizador(new Usuario("Demo", "demo@x.com", "hash"))));

        CapacidadDiaria capacidad = useCase.execute(BigDecimal.valueOf(4));

        assertThat(capacidad.getLimiteHoras()).isEqualByComparingTo("4");
    }

    @Test
    void actualizaLaCapacidadExistente() {
        capacidadExistente(6);

        CapacidadDiaria capacidad = useCase.execute(BigDecimal.valueOf(8));

        assertThat(capacidad.getLimiteHoras()).isEqualByComparingTo("8");
    }

    @Test
    void rechazaLimitesFueraDelRangoPermitido() {
        when(capacidadDiariaRepository.findCurrentByOrganizadorId(organizadorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(BigDecimal.valueOf(20)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void noPermiteBajarElLimitePorDebajoDeLoYaPlanificado() {
        capacidadExistente(6);
        LocalDate manana = LocalDate.now().plusDays(1);
        TreeMap<LocalDate, BigDecimal> carga = new TreeMap<>();
        carga.put(LocalDate.now(), new BigDecimal("5"));
        carga.put(manana, new BigDecimal("6"));
        when(subtareaRepository.horasPlanificadasPorDiaDesde(organizadorId, LocalDate.now())).thenReturn(carga);

        assertThatThrownBy(() -> useCase.execute(new BigDecimal("5.5")))
                .isInstanceOf(LimiteBajoCargaPlanificadaException.class)
                .satisfies(ex -> {
                    var conflicto = (LimiteBajoCargaPlanificadaException) ex;
                    assertThat(conflicto.getDiasSobrecargados()).containsOnlyKeys(manana);
                    assertThat(conflicto.getMessage()).contains("5.5 h").contains(": 6 h");
                });
        verify(capacidadDiariaRepository, never()).save(any());
    }

    @Test
    void permiteBajarElLimiteSiNingunDiaLoSupera() {
        capacidadExistente(6);
        TreeMap<LocalDate, BigDecimal> carga = new TreeMap<>();
        carga.put(LocalDate.now().plusDays(1), new BigDecimal("5.5"));
        when(subtareaRepository.horasPlanificadasPorDiaDesde(organizadorId, LocalDate.now())).thenReturn(carga);

        assertThat(useCase.execute(new BigDecimal("5.5")).getLimiteHoras()).isEqualByComparingTo("5.5");
    }

    @Test
    void subirElLimiteNoConsultaLaCargaPlanificada() {
        capacidadExistente(6);

        useCase.execute(BigDecimal.valueOf(10));

        verify(subtareaRepository, never()).horasPlanificadasPorDiaDesde(any(), any());
    }
}

package com.events.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.events.application.port.out.CapacidadDiariaRepositoryPort;
import com.events.application.port.out.CurrentOrganizadorPort;
import com.events.domain.entity.CapacidadDiaria;
import com.events.domain.entity.Organizador;
import com.events.domain.entity.Usuario;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetCapacidadUseCaseTest {

    private final CapacidadDiariaRepositoryPort capacidadDiariaRepository = mock(CapacidadDiariaRepositoryPort.class);
    private final CurrentOrganizadorPort currentOrganizador = mock(CurrentOrganizadorPort.class);
    private final GetCapacidadUseCase useCase = new GetCapacidadUseCase(capacidadDiariaRepository, currentOrganizador);

    @Test
    void devuelveNullCuandoNoHayCapacidadConfigurada() {
        UUID organizadorId = UUID.randomUUID();
        when(currentOrganizador.currentOrganizadorId()).thenReturn(organizadorId);
        when(capacidadDiariaRepository.findCurrentByOrganizadorId(organizadorId)).thenReturn(Optional.empty());

        assertThat(useCase.execute()).isNull();
    }

    @Test
    void devuelveLaCapacidadVigenteDelOrganizador() {
        UUID organizadorId = UUID.randomUUID();
        when(currentOrganizador.currentOrganizadorId()).thenReturn(organizadorId);
        CapacidadDiaria capacidad = new CapacidadDiaria(new Organizador(new Usuario("Demo", "demo@x.com", "hash")), LocalDate.now(), BigDecimal.valueOf(8));
        when(capacidadDiariaRepository.findCurrentByOrganizadorId(organizadorId)).thenReturn(Optional.of(capacidad));

        assertThat(useCase.execute()).isSameAs(capacidad);
    }
}

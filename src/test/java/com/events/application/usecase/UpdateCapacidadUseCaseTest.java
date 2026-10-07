package com.events.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.events.application.port.out.CapacidadDiariaRepositoryPort;
import com.events.application.port.out.CurrentOrganizadorPort;
import com.events.application.port.out.OrganizadorRepositoryPort;
import com.events.domain.entity.CapacidadDiaria;
import com.events.domain.entity.Organizador;
import com.events.domain.entity.Usuario;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UpdateCapacidadUseCaseTest {

    private final CapacidadDiariaRepositoryPort capacidadDiariaRepository = mock(CapacidadDiariaRepositoryPort.class);
    private final OrganizadorRepositoryPort organizadorRepository = mock(OrganizadorRepositoryPort.class);
    private final CurrentOrganizadorPort currentOrganizador = mock(CurrentOrganizadorPort.class);
    private final UpdateCapacidadUseCase useCase =
            new UpdateCapacidadUseCase(capacidadDiariaRepository, organizadorRepository, currentOrganizador);

    @Test
    void creaUnaCapacidadNuevaCuandoNoExisteUnaPrevia() {
        UUID organizadorId = UUID.randomUUID();
        when(currentOrganizador.currentOrganizadorId()).thenReturn(organizadorId);
        when(capacidadDiariaRepository.findCurrentByOrganizadorId(organizadorId)).thenReturn(Optional.empty());
        when(organizadorRepository.findById(organizadorId)).thenReturn(Optional.of(new Organizador(new Usuario("Demo", "demo@x.com", "hash"))));
        when(capacidadDiariaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CapacidadDiaria capacidad = useCase.execute(BigDecimal.valueOf(4));

        assertThat(capacidad.getLimiteHoras()).isEqualByComparingTo("4");
    }

    @Test
    void actualizaLaCapacidadExistente() {
        UUID organizadorId = UUID.randomUUID();
        when(currentOrganizador.currentOrganizadorId()).thenReturn(organizadorId);
        CapacidadDiaria existente = new CapacidadDiaria(new Organizador(new Usuario("Demo", "demo@x.com", "hash")), LocalDate.now().minusDays(1), BigDecimal.valueOf(6));
        when(capacidadDiariaRepository.findCurrentByOrganizadorId(organizadorId)).thenReturn(Optional.of(existente));
        when(capacidadDiariaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CapacidadDiaria capacidad = useCase.execute(BigDecimal.valueOf(8));

        assertThat(capacidad.getLimiteHoras()).isEqualByComparingTo("8");
    }

    @Test
    void rechazaLimitesFueraDelRangoPermitido() {
        UUID organizadorId = UUID.randomUUID();
        when(currentOrganizador.currentOrganizadorId()).thenReturn(organizadorId);
        when(capacidadDiariaRepository.findCurrentByOrganizadorId(organizadorId)).thenReturn(Optional.empty());
        when(organizadorRepository.findById(organizadorId)).thenReturn(Optional.of(new Organizador(new Usuario("Demo", "demo@x.com", "hash"))));

        assertThatThrownBy(() -> useCase.execute(BigDecimal.valueOf(20)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

package com.events.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.events.application.port.out.CapacidadDiariaRepositoryPort;
import com.events.application.port.out.EventoRepositoryPort;
import com.events.domain.entity.CapacidadDiaria;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CheckOrganizadorDataUseCaseTest {

    private final EventoRepositoryPort eventoRepository = mock(EventoRepositoryPort.class);
    private final CapacidadDiariaRepositoryPort capacidadDiariaRepository = mock(CapacidadDiariaRepositoryPort.class);
    private final CheckOrganizadorDataUseCase useCase = new CheckOrganizadorDataUseCase(eventoRepository, capacidadDiariaRepository);
    private final UUID organizadorId = UUID.randomUUID();

    @Test
    void tieneDatosSiTieneEventos() {
        when(eventoRepository.existsByOrganizadorId(organizadorId)).thenReturn(true);

        assertThat(useCase.execute(organizadorId)).isTrue();
    }

    @Test
    void tieneDatosSiConfiguroCapacidad() {
        when(capacidadDiariaRepository.findCurrentByOrganizadorId(organizadorId))
                .thenReturn(Optional.of(new CapacidadDiaria(organizadorId, LocalDate.now(), BigDecimal.valueOf(6))));

        assertThat(useCase.execute(organizadorId)).isTrue();
    }

    @Test
    void noTieneDatosSinEventosNiCapacidad() {
        when(capacidadDiariaRepository.findCurrentByOrganizadorId(organizadorId)).thenReturn(Optional.empty());

        assertThat(useCase.execute(organizadorId)).isFalse();
    }
}

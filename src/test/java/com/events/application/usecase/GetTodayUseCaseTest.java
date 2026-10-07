package com.events.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.events.application.port.in.TodayGroups;
import com.events.application.port.out.CurrentOrganizadorPort;
import com.events.application.port.out.SubtareaRepositoryPort;
import com.events.domain.entity.EstadoSubtarea;
import com.events.domain.entity.Evento;
import com.events.domain.entity.Organizador;
import com.events.domain.entity.Usuario;
import com.events.domain.entity.Subtarea;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetTodayUseCaseTest {

    private final SubtareaRepositoryPort subtareaRepository = mock(SubtareaRepositoryPort.class);
    private final CurrentOrganizadorPort currentOrganizador = mock(CurrentOrganizadorPort.class);
    private final GetTodayUseCase useCase = new GetTodayUseCase(subtareaRepository, currentOrganizador);

    private Subtarea subtarea(String nombre, LocalDate fecha, double horas, EstadoSubtarea estado) {
        Evento evento = new Evento("Evento", "Social", "Cliente", null, null, null, null, new Organizador(new Usuario("Demo", "demo@x.com", "hash")));
        Subtarea subtarea = new Subtarea(nombre, fecha, BigDecimal.valueOf(horas));
        subtarea.asociarEvento(evento);
        if (estado == EstadoSubtarea.DONE) {
            subtarea.marcarHecha();
        }
        return subtarea;
    }

    @Test
    void agrupaPorVencidasHoyYProximas() {
        UUID organizadorId = UUID.randomUUID();
        when(currentOrganizador.currentOrganizadorId()).thenReturn(organizadorId);

        LocalDate hoy = LocalDate.now();
        Subtarea vencida = subtarea("Vencida", hoy.minusDays(2), 1, EstadoSubtarea.PENDING);
        Subtarea deHoy = subtarea("De hoy", hoy, 1, EstadoSubtarea.PENDING);
        Subtarea proxima = subtarea("Proxima", hoy.plusDays(3), 1, EstadoSubtarea.PENDING);
        Subtarea hecha = subtarea("Hecha", hoy.minusDays(1), 1, EstadoSubtarea.DONE);

        when(subtareaRepository.findByOrganizadorId(organizadorId))
                .thenReturn(List.of(vencida, deHoy, proxima, hecha));

        TodayGroups groups = useCase.execute(null, null);

        assertThat(groups.vencidas()).containsExactly(vencida);
        assertThat(groups.paraHoy()).containsExactly(deHoy);
        assertThat(groups.proximas()).containsExactly(proxima);
    }

    @Test
    void ordenaVencidasPorFechaMasAntiguaPrimero() {
        UUID organizadorId = UUID.randomUUID();
        when(currentOrganizador.currentOrganizadorId()).thenReturn(organizadorId);

        LocalDate hoy = LocalDate.now();
        Subtarea masReciente = subtarea("Reciente", hoy.minusDays(1), 1, EstadoSubtarea.PENDING);
        Subtarea masAntigua = subtarea("Antigua", hoy.minusDays(5), 1, EstadoSubtarea.PENDING);

        when(subtareaRepository.findByOrganizadorId(organizadorId))
                .thenReturn(List.of(masReciente, masAntigua));

        TodayGroups groups = useCase.execute(null, null);

        assertThat(groups.vencidas()).containsExactly(masAntigua, masReciente);
    }

    @Test
    void desempataPorMenorEsfuerzoCuandoLaFechaEsIgual() {
        UUID organizadorId = UUID.randomUUID();
        when(currentOrganizador.currentOrganizadorId()).thenReturn(organizadorId);

        LocalDate hoy = LocalDate.now();
        Subtarea pesada = subtarea("Pesada", hoy, 5, EstadoSubtarea.PENDING);
        Subtarea liviana = subtarea("Liviana", hoy, 1, EstadoSubtarea.PENDING);

        when(subtareaRepository.findByOrganizadorId(organizadorId))
                .thenReturn(List.of(pesada, liviana));

        TodayGroups groups = useCase.execute(null, null);

        assertThat(groups.paraHoy()).containsExactly(liviana, pesada);
    }

    @Test
    void excluyeSubtareasDone() {
        UUID organizadorId = UUID.randomUUID();
        when(currentOrganizador.currentOrganizadorId()).thenReturn(organizadorId);

        LocalDate hoy = LocalDate.now();
        Subtarea hecha = subtarea("Hecha", hoy, 1, EstadoSubtarea.DONE);
        when(subtareaRepository.findByOrganizadorId(organizadorId)).thenReturn(List.of(hecha));

        TodayGroups groups = useCase.execute(null, null);

        assertThat(groups.vencidas()).isEmpty();
        assertThat(groups.paraHoy()).isEmpty();
        assertThat(groups.proximas()).isEmpty();
    }

    @Test
    void filtraPorEstadoCuandoSeIndica() {
        UUID organizadorId = UUID.randomUUID();
        when(currentOrganizador.currentOrganizadorId()).thenReturn(organizadorId);

        LocalDate hoy = LocalDate.now();
        Subtarea pendiente = subtarea("Pendiente", hoy, 1, EstadoSubtarea.PENDING);
        Subtarea pospuesta = subtarea("Pospuesta", hoy, 1, EstadoSubtarea.PENDING);
        pospuesta.posponer("nota");

        when(subtareaRepository.findByOrganizadorId(organizadorId)).thenReturn(List.of(pendiente, pospuesta));

        TodayGroups groups = useCase.execute(null, EstadoSubtarea.POSTPONED);

        assertThat(groups.paraHoy()).containsExactly(pospuesta);
    }
}

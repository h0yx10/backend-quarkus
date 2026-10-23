package com.events.application.usecase;

import com.events.application.port.in.GetTodayPort;
import com.events.application.port.in.TodayGroups;
import com.events.application.port.out.CurrentOrganizadorPort;
import com.events.application.port.out.SubtareaRepositoryPort;
import com.events.domain.entity.EstadoSubtarea;
import com.events.domain.entity.Subtarea;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public class GetTodayUseCase implements GetTodayPort {

    private final SubtareaRepositoryPort subtareaRepository;
    private final CurrentOrganizadorPort currentOrganizador;

    public GetTodayUseCase(SubtareaRepositoryPort subtareaRepository, CurrentOrganizadorPort currentOrganizador) {
        this.subtareaRepository = subtareaRepository;
        this.currentOrganizador = currentOrganizador;
    }

    @Override
    public TodayGroups execute(java.util.UUID eventoIdFiltro, EstadoSubtarea estadoFiltro) {
        LocalDate hoy = LocalDate.now();
        Comparator<Subtarea> porFechaYEsfuerzo = Comparator.comparing(Subtarea::getFechaObjetivo)
                .thenComparing(Subtarea::getHorasEstimadas);

        List<Subtarea> activas = subtareaRepository.findByOrganizadorId(currentOrganizador.currentOrganizadorId())
                .stream()
                .filter(s -> s.getEstado() != EstadoSubtarea.DONE)
                .filter(s -> eventoIdFiltro == null || eventoIdFiltro.equals(s.getEvento().getId()))
                .filter(s -> estadoFiltro == null || estadoFiltro == s.getEstado())
                .toList();

        List<Subtarea> vencidas = activas.stream()
                .filter(s -> s.getFechaObjetivo().isBefore(hoy))
                .sorted(porFechaYEsfuerzo)
                .toList();

        List<Subtarea> paraHoy = activas.stream()
                .filter(s -> s.getFechaObjetivo().isEqual(hoy))
                .sorted(Comparator.comparing(Subtarea::getHorasEstimadas))
                .toList();

        List<Subtarea> proximas = activas.stream()
                .filter(s -> s.getFechaObjetivo().isAfter(hoy))
                .sorted(porFechaYEsfuerzo)
                .toList();

        return new TodayGroups(vencidas, paraHoy, proximas);
    }
}

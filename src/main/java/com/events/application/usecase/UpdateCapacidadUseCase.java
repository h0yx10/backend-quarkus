package com.events.application.usecase;

import com.events.application.port.in.UpdateCapacidadPort;
import com.events.application.port.out.CapacidadDiariaRepositoryPort;
import com.events.application.port.out.CurrentOrganizadorPort;
import com.events.application.port.out.OrganizadorRepositoryPort;
import com.events.application.port.out.SubtareaRepositoryPort;
import com.events.domain.entity.CapacidadDiaria;
import com.events.domain.entity.Organizador;
import com.events.domain.exception.LimiteBajoCargaPlanificadaException;
import com.events.domain.exception.OrganizadorNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

public class UpdateCapacidadUseCase implements UpdateCapacidadPort {

    private final CapacidadDiariaRepositoryPort capacidadDiariaRepository;
    private final OrganizadorRepositoryPort organizadorRepository;
    private final SubtareaRepositoryPort subtareaRepository;
    private final CurrentOrganizadorPort currentOrganizador;

    public UpdateCapacidadUseCase(CapacidadDiariaRepositoryPort capacidadDiariaRepository,
                                   OrganizadorRepositoryPort organizadorRepository,
                                   SubtareaRepositoryPort subtareaRepository,
                                   CurrentOrganizadorPort currentOrganizador) {
        this.capacidadDiariaRepository = capacidadDiariaRepository;
        this.organizadorRepository = organizadorRepository;
        this.subtareaRepository = subtareaRepository;
        this.currentOrganizador = currentOrganizador;
    }

    @Override
    public CapacidadDiaria execute(BigDecimal limiteHoras) {
        CapacidadDiaria.requireRangoValido(limiteHoras);
        UUID organizadorId = currentOrganizador.currentOrganizadorId();

        Optional<CapacidadDiaria> existente = capacidadDiariaRepository.findCurrentByOrganizadorId(organizadorId);
        BigDecimal limiteActual = existente.map(CapacidadDiaria::getLimiteHoras).orElse(CapacidadDiaria.LIMITE_POR_DEFECTO);
        if (limiteHoras.compareTo(limiteActual) < 0) {
            validarCargaPlanificada(organizadorId, limiteHoras);
        }

        if (existente.isPresent()) {
            CapacidadDiaria capacidad = existente.get();
            capacidad.actualizarLimite(limiteHoras, LocalDate.now());
            return capacidadDiariaRepository.save(capacidad);
        }

        Organizador organizador = organizadorRepository.findById(organizadorId)
                .orElseThrow(() -> new OrganizadorNotFoundException("No encontramos el usuario autenticado."));
        return capacidadDiariaRepository.save(new CapacidadDiaria(organizador, LocalDate.now(), limiteHoras));
    }

    /**
     * Bajar el limite no puede dejar dias sobrecargados sin aviso: desde hoy, ningun dia puede tener
     * mas horas no DONE que el nuevo limite. Los dias pasados no cuentan; subir el limite siempre se permite.
     */
    private void validarCargaPlanificada(UUID organizadorId, BigDecimal nuevoLimite) {
        SortedMap<LocalDate, BigDecimal> sobrecargados = new TreeMap<>();
        subtareaRepository.horasPlanificadasPorDiaDesde(organizadorId, LocalDate.now()).forEach((fecha, horas) -> {
            if (horas.compareTo(nuevoLimite) > 0) {
                sobrecargados.put(fecha, horas);
            }
        });
        if (!sobrecargados.isEmpty()) {
            throw new LimiteBajoCargaPlanificadaException(nuevoLimite, sobrecargados);
        }
    }
}

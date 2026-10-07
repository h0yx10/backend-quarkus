package com.events.infrastructure.adapter.out.persistence;

import com.events.application.port.out.*;
import com.events.domain.entity.*;
import com.events.domain.exception.*;
import com.events.infrastructure.adapter.out.persistence.entity.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@ApplicationScoped
@Transactional
public class CapacidadDiariaPersistenceAdapter implements CapacidadDiariaRepositoryPort {
    @Inject EntityManager em;
    @Inject PersistenceMapper mapper;
    public CapacidadDiaria save(CapacidadDiaria value) {
        var e=mapper.write(value); if(e.id==null) em.persist(e); em.flush(); return mapper.capacidad(e);
    }
    public Optional<CapacidadDiaria> findCurrentByOrganizadorId(UUID owner) {
        return em.createQuery("select c from CapacidadDiariaEntity c where c.organizador.id=:owner order by c.fecha desc",CapacidadDiariaEntity.class)
                .setParameter("owner",owner).setMaxResults(1).getResultStream().findFirst().map(mapper::capacidad);
    }
}

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
public class EventoPersistenceAdapter implements EventoRepositoryPort {
    @Inject EntityManager em;
    @Inject PersistenceMapper mapper;
    public Evento save(Evento value) {
        var e=mapper.write(value); if(e.id==null) em.persist(e); em.flush(); return mapper.evento(e);
    }
    public Optional<Evento> findByIdAndOrganizadorId(UUID id, UUID organizadorId) {
        return em.createQuery("select distinct e from EventoEntity e left join fetch e.subtareas where e.id=:id and e.organizador.id=:owner", EventoEntity.class)
                .setParameter("id",id).setParameter("owner",organizadorId).getResultStream().findFirst().map(mapper::evento);
    }
    public List<Evento> findByOrganizadorId(UUID owner) {
        return em.createQuery("select distinct e from EventoEntity e left join fetch e.subtareas where e.organizador.id=:owner", EventoEntity.class)
                .setParameter("owner",owner).getResultList().stream().map(mapper::evento).toList();
    }
    public boolean existsByIdAndOrganizadorId(UUID id, UUID owner) {
        return em.createQuery("select count(e) from EventoEntity e where e.id=:id and e.organizador.id=:owner",Long.class)
                .setParameter("id",id).setParameter("owner",owner).getSingleResult()>0;
    }
    public void deleteById(UUID id) { var e=em.find(EventoEntity.class,id); if(e!=null) em.remove(e); em.flush(); }
}

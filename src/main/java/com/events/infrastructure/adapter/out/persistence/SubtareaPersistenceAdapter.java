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
public class SubtareaPersistenceAdapter implements SubtareaRepositoryPort {
    @Inject EntityManager em;
    @Inject PersistenceMapper mapper;
    public Subtarea save(Subtarea value) {
        var e=mapper.write(value); if(e.id==null) em.persist(e); em.flush(); return mapper.subtarea(e);
    }
    public Optional<Subtarea> findByIdAndOrganizadorId(UUID id, UUID owner) {
        return em.createQuery("select s from SubtareaEntity s join fetch s.evento e join fetch e.organizador where s.id=:id and e.organizador.id=:owner",SubtareaEntity.class)
                .setParameter("id",id).setParameter("owner",owner).getResultStream().findFirst().map(mapper::subtarea);
    }
    public List<Subtarea> findByEventoId(UUID id) {
        return em.createQuery("select s from SubtareaEntity s where s.evento.id=:id",SubtareaEntity.class)
                .setParameter("id",id).getResultList().stream().map(mapper::subtarea).toList();
    }
    public List<Subtarea> findByOrganizadorId(UUID id) {
        return em.createQuery("select s from SubtareaEntity s where s.evento.organizador.id=:id",SubtareaEntity.class)
                .setParameter("id",id).getResultList().stream().map(mapper::subtarea).toList();
    }
    public void deleteById(UUID id) { var e=em.find(SubtareaEntity.class,id); if(e!=null) em.remove(e); em.flush(); }
    public BigDecimal sumHorasPlanificadas(UUID owner, LocalDate fecha, UUID excluded) {
        String jpql="select coalesce(sum(s.horasEstimadas),0) from SubtareaEntity s where s.evento.organizador.id=:owner and s.fechaObjetivo=:fecha and s.estado<>:done";
        if(excluded!=null) jpql+=" and s.id<>:excluded";
        var q=em.createQuery(jpql,BigDecimal.class).setParameter("owner",owner).setParameter("fecha",fecha).setParameter("done",EstadoSubtarea.DONE);
        if(excluded!=null) q.setParameter("excluded",excluded); return q.getSingleResult();
    }
}

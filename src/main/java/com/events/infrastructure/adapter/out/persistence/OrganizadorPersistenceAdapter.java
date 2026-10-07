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
public class OrganizadorPersistenceAdapter implements OrganizadorRepositoryPort {
    @Inject EntityManager em;
    @Inject PersistenceMapper mapper;
    public Optional<Organizador> findById(UUID id) { return Optional.ofNullable(em.find(OrganizadorEntity.class,id)).map(mapper::organizador); }
    public Optional<Organizador> findByUsuarioId(UUID id) {
        return em.createQuery("select o from OrganizadorEntity o where o.usuario.id=:id", OrganizadorEntity.class)
                .setParameter("id",id).getResultStream().findFirst().map(mapper::organizador);
    }
}

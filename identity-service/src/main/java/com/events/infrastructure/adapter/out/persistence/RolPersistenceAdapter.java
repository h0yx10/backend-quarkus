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
public class RolPersistenceAdapter implements RolRepositoryPort {
    @Inject EntityManager em;
    @Inject PersistenceMapper mapper;
    public Optional<Rol> findByNombre(NombreRol nombre) {
        return em.createQuery("select r from RolEntity r where r.nombre=:nombre", RolEntity.class)
                .setParameter("nombre", nombre).getResultStream().findFirst().map(mapper::rol);
    }
    public Rol save(Rol role) {
        var e = new RolEntity(); e.nombre=role.getNombre(); em.persist(e); em.flush(); return mapper.rol(e);
    }
    public void lockAdminGuard() {
        em.createQuery("select r from RolEntity r where r.nombre=:nombre", RolEntity.class).setParameter("nombre", NombreRol.ADMIN)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultStream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Falta la semilla ADMIN."));
    }
}

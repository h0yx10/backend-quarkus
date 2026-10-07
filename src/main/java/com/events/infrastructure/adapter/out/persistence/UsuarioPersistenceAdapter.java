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
public class UsuarioPersistenceAdapter implements UsuarioRepositoryPort {
    @Inject EntityManager em;
    @Inject PersistenceMapper mapper;
    public Usuario save(Usuario value) {
        try {
            var e = mapper.write(value); if (e.id == null) em.persist(e); em.flush(); return mapper.usuario(e);
        } catch (RuntimeException ex) {
            if (SqlErrors.hasState(ex, "23505")) throw new CorreoYaRegistradoException("Ya existe una cuenta con ese correo.");
            throw ex;
        }
    }
    public Optional<Usuario> findById(UUID id) { return Optional.ofNullable(em.find(UsuarioEntity.class, id)).map(mapper::usuario); }
    public Optional<Usuario> findByCorreo(String correo) {
        return em.createQuery("select u from UsuarioEntity u where lower(u.correo) = lower(:email)", UsuarioEntity.class)
                .setParameter("email", correo).getResultStream().findFirst().map(mapper::usuario);
    }
    public boolean existsByCorreo(String correo) { return findByCorreo(correo).isPresent(); }
    public List<Usuario> findAll() { return em.createQuery("select u from UsuarioEntity u", UsuarioEntity.class).getResultList().stream().map(mapper::usuario).toList(); }
    public Optional<Usuario> findByIdForUpdate(UUID id) { return Optional.ofNullable(em.find(UsuarioEntity.class, id, LockModeType.PESSIMISTIC_WRITE)).map(mapper::usuario); }
    public long countActiveAdmins() {
        return em.createQuery("select count(u) from UsuarioEntity u join u.roles r left join u.organizador o "
                + "where r.nombre = :role and u.passwordHash is not null and (o is null or o.activo = true)", Long.class)
                .setParameter("role", NombreRol.ADMIN).getSingleResult();
    }
    public boolean hasBusinessData(UUID id) {
        return Boolean.TRUE.equals(em.createNativeQuery("select exists(select 1 from eventos e join organizadores o on o.id=e.organizador_id where o.usuario_id=:id) "
                + "or exists(select 1 from capacidades_diarias c join organizadores o on o.id=c.organizador_id where o.usuario_id=:id)")
                .setParameter("id", id).getSingleResult());
    }
    public void delete(Usuario value) {
        try { em.remove(em.find(UsuarioEntity.class, value.getId())); em.flush(); }
        catch (RuntimeException ex) {
            if (SqlErrors.hasState(ex, "23503")) throw new UsuarioConflictException("No se puede eliminar un usuario con datos asociados.");
            throw ex;
        }
    }
}

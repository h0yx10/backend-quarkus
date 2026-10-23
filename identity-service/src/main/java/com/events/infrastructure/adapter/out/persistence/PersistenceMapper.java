package com.events.infrastructure.adapter.out.persistence;

import com.events.domain.entity.*;
import com.events.infrastructure.adapter.out.persistence.entity.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.util.*;

/** Contexto por agregado: no expone proxies ORM ni recorre indefinidamente relaciones bidireccionales. */
@ApplicationScoped
public class PersistenceMapper {
    @Inject EntityManager em;
    private static class Context {
        final Map<UUID, Usuario> users = new HashMap<>();
        final Map<UUID, Organizador> organizers = new HashMap<>();
    }
    public Usuario usuario(UsuarioEntity e) { return usuario(e, new Context()); }
    public Organizador organizador(OrganizadorEntity e) { return organizador(e, new Context()); }
    public Rol rol(RolEntity e) { e = org.hibernate.Hibernate.unproxy(e, RolEntity.class); return Rol.reconstituir(e.id, e.nombre); }
    private Usuario usuario(UsuarioEntity e, Context c) {
        e = org.hibernate.Hibernate.unproxy(e, UsuarioEntity.class);
        if (c.users.containsKey(e.id)) return c.users.get(e.id);
        Set<Rol> roles = new HashSet<>(); e.roles.forEach(r -> roles.add(rol(r)));
        Usuario u = Usuario.reconstituir(e.id, e.nombre, e.correo, e.passwordHash, e.createdAt, roles);
        c.users.put(e.id, u);
        if (e.organizador != null) u.asociarOrganizador(organizador(e.organizador, c));
        return u;
    }
    private Organizador organizador(OrganizadorEntity e, Context c) {
        e = org.hibernate.Hibernate.unproxy(e, OrganizadorEntity.class);
        if (c.organizers.containsKey(e.id)) return c.organizers.get(e.id);
        Usuario u = usuario(e.usuario, c);
        if (c.organizers.containsKey(e.id)) return c.organizers.get(e.id);
        Organizador o = Organizador.reconstituir(e.id, u, e.activo);
        c.organizers.put(e.id, o); u.asociarOrganizador(o); return o;
    }
    public UsuarioEntity write(Usuario u) {
        UsuarioEntity e = u.getId() == null ? new UsuarioEntity() : em.find(UsuarioEntity.class, u.getId());
        e = org.hibernate.Hibernate.unproxy(e, UsuarioEntity.class);
        e.nombre = u.getNombre(); e.correo = u.getCorreo(); e.passwordHash = u.getPasswordHash();
        if (e.createdAt == null) e.createdAt = u.getCreatedAt();
        e.roles.clear(); for (Rol role : u.getRoles()) e.roles.add(em.getReference(RolEntity.class, role.getId()));
        if (u.getOrganizador() != null) {
            OrganizadorEntity o = e.organizador == null ? new OrganizadorEntity() : e.organizador;
            o.usuario = e; o.activo = u.getOrganizador().isActivo(); e.organizador = o;
        }
        return e;
    }
}

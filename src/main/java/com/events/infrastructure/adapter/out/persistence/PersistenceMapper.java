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
        final Map<UUID, Evento> events = new HashMap<>();
        final Map<UUID, Subtarea> tasks = new HashMap<>();
    }
    public Usuario usuario(UsuarioEntity e) { return usuario(e, new Context()); }
    public Organizador organizador(OrganizadorEntity e) { return organizador(e, new Context()); }
    public Evento evento(EventoEntity e) { return evento(e, new Context()); }
    public Subtarea subtarea(SubtareaEntity e) { return subtarea(e, new Context()); }
    public Rol rol(RolEntity e) { e = org.hibernate.Hibernate.unproxy(e, RolEntity.class); return Rol.reconstituir(e.id, e.nombre); }
    public CapacidadDiaria capacidad(CapacidadDiariaEntity e) {
        e = org.hibernate.Hibernate.unproxy(e, CapacidadDiariaEntity.class);
        return CapacidadDiaria.reconstituir(e.id, organizador(e.organizador), e.fecha, e.limiteHoras);
    }
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
    private Evento evento(EventoEntity e, Context c) {
        e = org.hibernate.Hibernate.unproxy(e, EventoEntity.class);
        if (c.events.containsKey(e.id)) return c.events.get(e.id);
        Evento value = Evento.reconstituir(e.id, e.nombre, e.tipo, e.cliente, e.contactoCliente,
                e.fechaHora, e.lugar, e.plazoLimite, organizador(e.organizador, c));
        c.events.put(e.id, value);
        e.subtareas.forEach(t -> value.agregarSubtarea(subtarea(t, c)));
        return value;
    }
    private Subtarea subtarea(SubtareaEntity e, Context c) {
        e = org.hibernate.Hibernate.unproxy(e, SubtareaEntity.class);
        if (c.tasks.containsKey(e.id)) return c.tasks.get(e.id);
        Evento event = evento(e.evento, c);
        if (c.tasks.containsKey(e.id)) return c.tasks.get(e.id);
        Subtarea value = Subtarea.reconstituir(e.id, e.nombre, e.descripcion, e.fechaObjetivo,
                e.horasEstimadas, e.estado, e.nota, e.doneAt, e.createdAt, event);
        c.tasks.put(e.id, value); return value;
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
    public EventoEntity write(Evento d) {
        EventoEntity e = d.getId() == null ? new EventoEntity() : em.find(EventoEntity.class, d.getId());
        e = org.hibernate.Hibernate.unproxy(e, EventoEntity.class);
        e.nombre = d.getNombre(); e.tipo = d.getTipo(); e.cliente = d.getCliente();
        e.contactoCliente = d.getContactoCliente(); e.fechaHora = d.getFechaHora();
        e.lugar = d.getLugar(); e.plazoLimite = d.getPlazoLimite();
        e.organizador = em.getReference(OrganizadorEntity.class, d.getOrganizador().getId());
        List<SubtareaEntity> tasks = new ArrayList<>();
        for (Subtarea t : d.getSubtareas()) { SubtareaEntity te = write(t, false); te.evento = e; tasks.add(te); }
        e.subtareas.clear(); e.subtareas.addAll(tasks); return e;
    }
    public SubtareaEntity write(Subtarea d) { return write(d, true); }
    private SubtareaEntity write(Subtarea d, boolean associateEvent) {
        SubtareaEntity e = d.getId() == null ? new SubtareaEntity() : em.find(SubtareaEntity.class, d.getId());
        e = org.hibernate.Hibernate.unproxy(e, SubtareaEntity.class);
        e.nombre = d.getNombre(); e.descripcion = d.getDescripcion(); e.fechaObjetivo = d.getFechaObjetivo();
        e.horasEstimadas = d.getHorasEstimadas(); e.estado = d.getEstado(); e.nota = d.getNota(); e.doneAt = d.getDoneAt();
        if (e.createdAt == null) e.createdAt = d.getCreatedAt();
        if (associateEvent) e.evento = em.getReference(EventoEntity.class, d.getEvento().getId());
        return e;
    }
    public CapacidadDiariaEntity write(CapacidadDiaria d) {
        CapacidadDiariaEntity e = d.getId() == null ? new CapacidadDiariaEntity() : em.find(CapacidadDiariaEntity.class, d.getId());
        e = org.hibernate.Hibernate.unproxy(e, CapacidadDiariaEntity.class);
        e.organizador = em.getReference(OrganizadorEntity.class, d.getOrganizador().getId());
        e.fecha = d.getFecha(); e.limiteHoras = d.getLimiteHoras(); return e;
    }
}

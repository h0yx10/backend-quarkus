package com.events.domain.entity;

import java.time.LocalDateTime;
import java.util.*;

/** Cuenta de login. Los roles pertenecen al usuario, independientemente de su perfil. */

public class Usuario {

    private UUID id;

    private String nombre;

    private String correo;

    private String passwordHash;

    private LocalDateTime createdAt;

    private Organizador organizador;

    private Set<Rol> roles = new HashSet<>();

    protected Usuario() { }
    public Usuario(String nombre, String correo, String passwordHash) {
        actualizarDatos(nombre, correo);
        this.passwordHash = passwordHash;
    }

    void onCreate() { createdAt = LocalDateTime.now(); }
    public Organizador habilitarComoOrganizador() {
        if (organizador == null) organizador = new Organizador(this);
        return organizador;
    }
    public void actualizarDatos(String nombre, String correo) {
        if (nombre != null) {
            String valor = nombre.trim();
            if (valor.isEmpty() || valor.length() > 120)
                throw new IllegalArgumentException("El nombre es obligatorio y admite hasta 120 caracteres.");
            this.nombre = valor;
        }
        if (correo != null) this.correo = normalizarCorreo(correo);
    }
    public static String normalizarCorreo(String correo) {
        String valor = correo.trim().toLowerCase(Locale.ROOT);
        if (valor.isEmpty() || valor.length() > 180)
            throw new IllegalArgumentException("El correo es obligatorio y admite hasta 180 caracteres.");
        return valor;
    }
    public void cambiarPasswordHash(String hash) { this.passwordHash = Objects.requireNonNull(hash); }
    public void asignarRol(Rol rol) { roles.add(Objects.requireNonNull(rol)); }
    public void reemplazarRoles(Set<Rol> nuevos) { roles.clear(); roles.addAll(nuevos); }
    public boolean tieneRol(NombreRol nombre) { return roles.stream().anyMatch(r -> r.getNombre() == nombre); }
    public boolean isActivo() { return organizador == null || organizador.isActivo(); }
    public boolean puedeIniciarSesion() { return isActivo() && passwordHash != null; }
    public UUID getId() { return id; }
    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }
    public String getPasswordHash() { return passwordHash; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Set<Rol> getRoles() { return Collections.unmodifiableSet(roles); }
    public Organizador getOrganizador() { return organizador; }

    /** Reconstruye un estado persistido sin ejecutar reglas ni callbacks de creacion. */
    public static Usuario reconstituir(UUID id, String nombre, String correo, String passwordHash, LocalDateTime createdAt, Set<Rol> roles) {
        Usuario value = new Usuario();
        value.id = id;
        value.nombre = nombre;
        value.correo = correo;
        value.passwordHash = passwordHash;
        value.createdAt = createdAt;
        value.roles = new java.util.HashSet<>(roles);
        return value;
    }
    public void asociarOrganizador(Organizador perfil) {
        if (perfil != null && perfil.getUsuario() != this) throw new IllegalArgumentException("Perfil de otro usuario.");
        this.organizador = perfil;
    }
}

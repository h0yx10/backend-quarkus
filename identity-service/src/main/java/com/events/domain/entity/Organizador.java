package com.events.domain.entity;

import java.util.UUID;

/** Perfil opcional 1:1; su UUID es independiente del UUID de la cuenta. */

public class Organizador {

    private UUID id;

    private Usuario usuario;

    private boolean activo = true;
    protected Organizador() { }
    public Organizador(Usuario usuario) { this.usuario = usuario; }
    public void desactivar() { activo = false; }
    public void cambiarActivo(boolean activo) { this.activo = activo; }
    public UUID getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getNombre() { return usuario.getNombre(); }
    public String getCorreo() { return usuario.getCorreo(); }
    public boolean isActivo() { return activo; }

    /** Reconstruye un estado persistido sin ejecutar reglas ni callbacks de creacion. */
    public static Organizador reconstituir(UUID id, Usuario usuario, boolean activo) {
        Organizador value = new Organizador();
        value.id = id;
        value.usuario = usuario;
        value.activo = activo;
        return value;
    }
}

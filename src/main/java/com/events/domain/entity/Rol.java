package com.events.domain.entity;

import java.util.UUID;

public class Rol {

    private UUID id;

    private NombreRol nombre;

    protected Rol() {
    }

    public Rol(NombreRol nombre) {
        this.nombre = nombre;
    }

    public UUID getId() {
        return id;
    }

    public NombreRol getNombre() {
        return nombre;
    }

    /** Reconstruye un estado persistido sin ejecutar reglas ni callbacks de creacion. */
    public static Rol reconstituir(UUID id, NombreRol nombre) {
        Rol value = new Rol();
        value.id = id;
        value.nombre = nombre;
        return value;
    }
}

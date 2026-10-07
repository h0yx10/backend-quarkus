package com.events.domain.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Evento {

    private UUID id;

    private String nombre;

    private String tipo;

    private String cliente;

    private String contactoCliente;

    private LocalDateTime fechaHora;

    private String lugar;

    private LocalDateTime plazoLimite;

    private Organizador organizador;

    private List<Subtarea> subtareas = new ArrayList<>();

    protected Evento() {
    }

    public Evento(String nombre, String tipo, String cliente, String contactoCliente,
                  LocalDateTime fechaHora, String lugar, LocalDateTime plazoLimite,
                  Organizador organizador) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.cliente = cliente;
        this.contactoCliente = contactoCliente;
        this.fechaHora = fechaHora;
        this.lugar = lugar;
        this.plazoLimite = plazoLimite;
        this.organizador = organizador;
    }

    public void agregarSubtarea(Subtarea subtarea) {
        subtareas.add(subtarea);
        subtarea.asociarEvento(this);
    }

    public void actualizar(String nombre, String tipo, String cliente, String contactoCliente,
                            LocalDateTime fechaHora, String lugar, LocalDateTime plazoLimite) {
        if (nombre != null) {
            this.nombre = nombre;
        }
        if (tipo != null) {
            this.tipo = tipo;
        }
        if (cliente != null) {
            this.cliente = cliente;
        }
        if (contactoCliente != null) {
            this.contactoCliente = contactoCliente;
        }
        if (fechaHora != null) {
            this.fechaHora = fechaHora;
        }
        if (lugar != null) {
            this.lugar = lugar;
        }
        if (plazoLimite != null) {
            this.plazoLimite = plazoLimite;
        }
    }

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getTipo() {
        return tipo;
    }

    public String getCliente() {
        return cliente;
    }

    public String getContactoCliente() {
        return contactoCliente;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public String getLugar() {
        return lugar;
    }

    public LocalDateTime getPlazoLimite() {
        return plazoLimite;
    }

    public Organizador getOrganizador() {
        return organizador;
    }

    public List<Subtarea> getSubtareas() {
        return subtareas;
    }

    /** Reconstruye un estado persistido sin ejecutar reglas ni callbacks de creacion. */
    public static Evento reconstituir(UUID id, String nombre, String tipo, String cliente, String contactoCliente, LocalDateTime fechaHora, String lugar, LocalDateTime plazoLimite, Organizador organizador) {
        Evento value = new Evento();
        value.id = id;
        value.nombre = nombre;
        value.tipo = tipo;
        value.cliente = cliente;
        value.contactoCliente = contactoCliente;
        value.fechaHora = fechaHora;
        value.lugar = lugar;
        value.plazoLimite = plazoLimite;
        value.organizador = organizador;
        return value;
    }
}

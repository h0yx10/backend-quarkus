package com.events.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class Subtarea {

    private UUID id;

    private String nombre;

    private String descripcion;

    private LocalDate fechaObjetivo;

    private BigDecimal horasEstimadas;

    private EstadoSubtarea estado;

    private String nota;

    private LocalDateTime doneAt;

    private LocalDateTime createdAt;

    private Evento evento;

    protected Subtarea() {
    }

    public Subtarea(String nombre, LocalDate fechaObjetivo, BigDecimal horasEstimadas) {
        this(nombre, null, fechaObjetivo, horasEstimadas);
    }

    public Subtarea(String nombre, String descripcion, LocalDate fechaObjetivo, BigDecimal horasEstimadas) {
        requireHorasPositivas(horasEstimadas);
        if (descripcion != null && descripcion.length() > 255) {
            throw new IllegalArgumentException("La descripcion puede tener maximo 255 caracteres.");
        }
        this.descripcion = descripcion;
        this.nombre = nombre;
        this.fechaObjetivo = fechaObjetivo;
        this.horasEstimadas = horasEstimadas;
        this.estado = EstadoSubtarea.PENDING;
    }

    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public void asociarEvento(Evento evento) {
        this.evento = evento;
    }

    public void actualizar(String nombre, LocalDate fechaObjetivo, BigDecimal horasEstimadas) {
        actualizar(nombre, null, fechaObjetivo, horasEstimadas);
    }

    public void actualizar(String nombre, String descripcion, LocalDate fechaObjetivo, BigDecimal horasEstimadas) {
        if (descripcion != null && descripcion.length() > 255) {
            throw new IllegalArgumentException("La descripcion puede tener maximo 255 caracteres.");
        }
        if (horasEstimadas != null) {
            requireHorasPositivas(horasEstimadas);
            this.horasEstimadas = horasEstimadas;
        }
        if (nombre != null) {
            this.nombre = nombre;
        }
        if (descripcion != null) {
            this.descripcion = descripcion;
        }
        if (fechaObjetivo != null) {
            this.fechaObjetivo = fechaObjetivo;
        }
    }

    public void reprogramar(LocalDate nuevaFecha) {
        this.fechaObjetivo = nuevaFecha;
    }

    public void marcarHecha() {
        this.estado = EstadoSubtarea.DONE;
        this.doneAt = LocalDateTime.now();
    }

    public void posponer(String nota) {
        this.estado = EstadoSubtarea.POSTPONED;
        this.nota = nota;
        this.doneAt = null;
    }

    public void reabrir() {
        this.estado = EstadoSubtarea.PENDING;
        this.doneAt = null;
    }

    private static void requireHorasPositivas(BigDecimal horas) {
        if (horas == null || horas.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Las horas estimadas deben ser mayores a 0.");
        }
    }

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public LocalDate getFechaObjetivo() {
        return fechaObjetivo;
    }

    public BigDecimal getHorasEstimadas() {
        return horasEstimadas;
    }

    public EstadoSubtarea getEstado() {
        return estado;
    }

    public String getNota() {
        return nota;
    }

    public LocalDateTime getDoneAt() {
        return doneAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Evento getEvento() {
        return evento;
    }

    /** Reconstruye un estado persistido sin ejecutar reglas ni callbacks de creacion. */
    public static Subtarea reconstituir(UUID id, String nombre, String descripcion, LocalDate fechaObjetivo, BigDecimal horasEstimadas, EstadoSubtarea estado, String nota, LocalDateTime doneAt, LocalDateTime createdAt, Evento evento) {
        Subtarea value = new Subtarea();
        value.id = id;
        value.nombre = nombre;
        value.descripcion = descripcion;
        value.fechaObjetivo = fechaObjetivo;
        value.horasEstimadas = horasEstimadas;
        value.estado = estado;
        value.nota = nota;
        value.doneAt = doneAt;
        value.createdAt = createdAt;
        value.evento = evento;
        return value;
    }
}

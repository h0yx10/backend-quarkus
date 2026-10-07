package com.events.infrastructure.adapter.out.persistence.entity;

import com.events.domain.entity.NombreRol;
import com.events.domain.entity.EstadoSubtarea;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "subtareas")
public class SubtareaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(nullable = false, length = 180)
    public String nombre;

    @Column(name = "descripcion", length = 255)
    public String descripcion;

    @Column(name = "fecha_objetivo", nullable = false)
    public LocalDate fechaObjetivo;

    @Column(name = "horas_estimadas", nullable = false, precision = 8, scale = 2)
    public BigDecimal horasEstimadas;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public EstadoSubtarea estado;

    @Column(length = 1000)
    public String nota;

    @Column(name = "done_at")
    public LocalDateTime doneAt;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    public EventoEntity evento;

    public SubtareaEntity() { }
    @jakarta.persistence.PrePersist
    void initializeCreatedAt() { if (createdAt == null) createdAt = java.time.LocalDateTime.now(java.time.ZoneId.of("America/Bogota")); }
}

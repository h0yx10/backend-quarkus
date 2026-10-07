package com.events.infrastructure.adapter.out.persistence.entity;

import com.events.domain.entity.NombreRol;
import com.events.domain.entity.EstadoSubtarea;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Guarda el limite diario de horas configurado por un organizador (US-12).
 * El backlog define un unico limite por organizador (no por fecha especifica), pero la tabla
 * conserva la fecha de vigencia para permitir un historial; el limite vigente es siempre el de
 * la fila mas reciente para ese organizador.
 */
@Entity
@Table(name = "capacidades_diarias", uniqueConstraints = @UniqueConstraint(columnNames = {"organizador_id", "fecha"}))
public class CapacidadDiariaEntity {

    public static final BigDecimal LIMITE_MINIMO = BigDecimal.ONE;
    public static final BigDecimal LIMITE_MAXIMO = BigDecimal.valueOf(16);
    public static final BigDecimal LIMITE_POR_DEFECTO = BigDecimal.valueOf(6);

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizador_id", nullable = false)
    public OrganizadorEntity organizador;

    @Column(nullable = false)
    public LocalDate fecha;

    @Column(name = "limite_horas", nullable = false, precision = 8, scale = 2)
    public BigDecimal limiteHoras;

    public CapacidadDiariaEntity() { }
}

package com.events.infrastructure.adapter.out.persistence.entity;

import com.events.domain.entity.NombreRol;
import com.events.domain.entity.EstadoSubtarea;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "eventos")
public class EventoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(nullable = false, length = 180)
    public String nombre;

    @Column(nullable = false, length = 100)
    public String tipo;

    @Column(length = 180)
    public String cliente;

    @Column(name = "contacto_cliente", length = 180)
    public String contactoCliente;

    @Column(name = "fecha_hora", nullable = false)
    public LocalDateTime fechaHora;

    @Column(length = 240)
    public String lugar;

    @Column(name = "plazo_limite")
    public LocalDateTime plazoLimite;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizador_id", nullable = false)
    public OrganizadorEntity organizador;

    @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<SubtareaEntity> subtareas = new ArrayList<>();

    public EventoEntity() { }
}

package com.events.infrastructure.adapter.out.persistence.entity;

import com.events.domain.entity.NombreRol;
import com.events.domain.entity.EstadoSubtarea;

import jakarta.persistence.*;
import java.util.UUID;

/** Perfil opcional 1:1; su UUID es independiente del UUID de la cuenta. */
@Entity
@Table(name = "organizadores")
public class OrganizadorEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    public UsuarioEntity usuario;
    @Column(nullable = false)
    public boolean activo = true;
    public OrganizadorEntity() { }
}

package com.events.infrastructure.adapter.out.persistence.entity;

import com.events.domain.entity.NombreRol;
import com.events.domain.entity.EstadoSubtarea;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

/** Cuenta de login. Los roles pertenecen al usuario, independientemente de su perfil. */
@Entity
@Table(name = "usuarios")
public class UsuarioEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;
    @Column(nullable = false, length = 120)
    public String nombre;
    @Column(nullable = false, unique = true, length = 180)
    public String correo;
    @Column(name = "password_hash", nullable = false, length = 100)
    public String passwordHash;
    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;
    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    public OrganizadorEntity organizador;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "usuario_roles", joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "rol_id"))
    public Set<RolEntity> roles = new HashSet<>();

    public UsuarioEntity() { }
    @jakarta.persistence.PrePersist
    void initializeCreatedAt() { if (createdAt == null) createdAt = java.time.LocalDateTime.now(java.time.ZoneId.of("America/Bogota")); }
}

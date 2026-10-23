package com.events.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Mapeo para que Hibernate valide tambien el esquema de revocaciones persistidas. */
@Entity
@Table(name = "tokens_revocados")
public class RevokedTokenEntity {
    @Id
    @Column(name = "token_hash", length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected RevokedTokenEntity() { }
}

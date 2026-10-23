package com.events.infrastructure.adapter.out.persistence;

import com.events.application.port.out.TokenRevocationPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import javax.sql.DataSource;
import java.sql.SQLException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/** Guarda solo SHA-256 del JWT. PostgreSQL hace la revocacion compartida entre instancias. */
@ApplicationScoped
public class TokenRevocationPersistenceAdapter implements TokenRevocationPort {
    @Inject DataSource dataSource;

    @Override
    @Transactional
    public void revoke(String token, Instant expiresAt) {
        try (var connection = dataSource.getConnection()) {
            try(var insert = connection.prepareStatement("INSERT INTO tokens_revocados (token_hash, expires_at) VALUES (?, ?) ON CONFLICT (token_hash) DO NOTHING")) {
                insert.setString(1,hash(token)); insert.setObject(2,OffsetDateTime.ofInstant(expiresAt, ZoneOffset.UTC)); insert.executeUpdate();
            }
            try(var cleanup = connection.prepareStatement("DELETE FROM tokens_revocados WHERE expires_at < ?")) {
                cleanup.setObject(1,OffsetDateTime.ofInstant(Instant.now().minusSeconds(60),ZoneOffset.UTC)); cleanup.executeUpdate();
            }
        } catch(SQLException ex) { throw new IllegalStateException("No se pudo persistir la revocacion.",ex); }
    }
    @Override
    public boolean isRevoked(String token) {
        try(var c=dataSource.getConnection();var q=c.prepareStatement("SELECT EXISTS(SELECT 1 FROM tokens_revocados WHERE token_hash=?)")) {
            q.setString(1,hash(token)); try(var rows=q.executeQuery()) { rows.next(); return rows.getBoolean(1); }
        } catch(SQLException ex) { throw new IllegalStateException("No se pudo consultar la revocacion.",ex); }
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no esta disponible.", ex);
        }
    }
}

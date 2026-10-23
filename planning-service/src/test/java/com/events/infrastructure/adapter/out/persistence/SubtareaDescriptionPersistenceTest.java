package com.events.infrastructure.adapter.out.persistence;
import java.util.UUID;
import com.events.domain.entity.*;
import com.events.application.port.out.*;
import jakarta.inject.Inject;
import io.quarkus.test.junit.QuarkusTest;
import java.math.BigDecimal;
import java.time.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
@QuarkusTest
class SubtareaDescriptionPersistenceTest {
    @Inject EventoRepositoryPort events;
    @Inject SubtareaRepositoryPort tasks;
    @Inject javax.sql.DataSource dataSource;
    @Test
    void guardaDescriptionEnColumnaDescripcionYLaRecupera() throws Exception {
        // El organizador vive en identity-service: aqui basta con su id.
        var organizadorId=java.util.UUID.randomUUID();
        var evento=events.save(new Evento("Boda","Social",null,null,LocalDateTime.now(),null,null,organizadorId));
        var subtarea=new Subtarea("Catering","a".repeat(255),LocalDate.now(),BigDecimal.ONE);subtarea.asociarEvento(evento);
        var stored=tasks.save(subtarea);
        try(var c=dataSource.getConnection();var q=c.prepareStatement("select descripcion from subtareas where id=?")) {
            q.setObject(1,stored.getId());try(var rows=q.executeQuery()) { rows.next();assertThat(rows.getString(1)).isEqualTo("a".repeat(255)); }
        }
        var loaded=tasks.findByIdAndOrganizadorId(stored.getId(),organizadorId).orElseThrow();
        loaded.actualizar(null,"Descripcion editada",null,null);tasks.save(loaded);
        assertThat(tasks.findByIdAndOrganizadorId(stored.getId(),organizadorId).orElseThrow().getDescripcion()).isEqualTo("Descripcion editada");
        events.deleteById(evento.getId());
    }
}

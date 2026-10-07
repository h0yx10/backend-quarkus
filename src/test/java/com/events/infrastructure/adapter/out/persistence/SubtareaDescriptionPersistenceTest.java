package com.events.infrastructure.adapter.out.persistence;
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
    @Inject UsuarioRepositoryPort users;
    @Inject EventoRepositoryPort events;
    @Inject SubtareaRepositoryPort tasks;
    @Inject TransactionPort transaction;
    @Inject javax.sql.DataSource dataSource;
    @Test
    void guardaDescriptionEnColumnaDescripcionYLaRecupera() throws Exception {
        var usuario=new Usuario("Camila","persistencia@correo.com","hash"); usuario.habilitarComoOrganizador();
        var saved=users.save(usuario);
        var evento=events.save(new Evento("Boda","Social",null,null,LocalDateTime.now(),null,null,saved.getOrganizador()));
        var subtarea=new Subtarea("Catering","a".repeat(255),LocalDate.now(),BigDecimal.ONE);subtarea.asociarEvento(evento);
        var stored=tasks.save(subtarea);
        try(var c=dataSource.getConnection();var q=c.prepareStatement("select descripcion from subtareas where id=?")) {
            q.setObject(1,stored.getId());try(var rows=q.executeQuery()) { rows.next();assertThat(rows.getString(1)).isEqualTo("a".repeat(255)); }
        }
        var loaded=tasks.findByIdAndOrganizadorId(stored.getId(),saved.getOrganizador().getId()).orElseThrow();
        loaded.actualizar(null,"Descripcion editada",null,null);tasks.save(loaded);
        assertThat(tasks.findByIdAndOrganizadorId(stored.getId(),saved.getOrganizador().getId()).orElseThrow().getDescripcion()).isEqualTo("Descripcion editada");
        events.deleteById(evento.getId());users.delete(saved);
    }
}

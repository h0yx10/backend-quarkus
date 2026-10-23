package com.events.infrastructure.security;

import static com.events.support.HttpTestClient.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.events.application.port.in.CheckOrganizadorDataPort;
import com.events.application.port.in.ListEventosPort;
import com.events.application.port.out.CurrentOrganizadorPort;
import com.events.support.HttpTestClient;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * La identidad llega en los headers que Traefik copia de identity-service (forwardAuth):
 * sin ellos 401, sin rol ORGANIZADOR 403, y el organizador actual sale de X-Organizador-Id.
 */
@QuarkusTest
class GatewayHeadersSecurityTest {
    private final HttpTestClient mvc = new HttpTestClient();
    @Inject CurrentOrganizadorPort currentOrganizador;
    @InjectMock ListEventosPort listEventos;
    @InjectMock CheckOrganizadorDataPort checkOrganizadorData;

    @Test
    void sinHeadersDelGatewayDevuelve401ConElSobreJson() {
        mvc.perform(get("/api/events"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(RestAuthenticationErrorHandler.TOKEN_REQUIRED));
        verifyNoInteractions(listEventos);
    }

    @Test
    void sinRolOrganizadorDevuelve403() {
        mvc.perform(get("/api/events").header("X-User-Id", UUID.randomUUID().toString()).header("X-Roles", "ADMIN"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(RestAuthenticationErrorHandler.ACCESS_DENIED));
        verifyNoInteractions(listEventos);
    }

    @Test
    void conHeadersValidosResuelveElOrganizadorActual() {
        UUID organizadorId = UUID.randomUUID();
        when(listEventos.execute()).thenAnswer(invocation -> {
            assertThat(currentOrganizador.currentOrganizadorId()).isEqualTo(organizadorId);
            return List.of();
        });
        mvc.perform(get("/api/events").header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-Organizador-Id", organizadorId.toString()).header("X-Roles", "ORGANIZADOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void endpointInternoNoExigeIdentidad() {
        UUID organizadorId = UUID.randomUUID();
        when(checkOrganizadorData.execute(organizadorId)).thenReturn(true);
        mvc.perform(get("/internal/organizadores/" + organizadorId + "/datos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tieneDatos").value(true));
    }
}

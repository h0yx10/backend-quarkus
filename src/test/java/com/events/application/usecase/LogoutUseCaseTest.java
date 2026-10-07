package com.events.application.usecase;

import com.events.application.port.out.CurrentTokenPort;
import com.events.application.port.out.TokenRevocationPort;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class LogoutUseCaseTest {
    @Test
    void revocaSoloElTokenAutenticadoConSuExpiracion() {
        var current = mock(CurrentTokenPort.class);
        var revocations = mock(TokenRevocationPort.class);
        Instant expires = Instant.parse("2026-10-03T18:00:00Z");
        when(current.tokenValue()).thenReturn("jwt-de-la-peticion");
        when(current.expiresAt()).thenReturn(expires);
        new LogoutUseCase(current, revocations).execute();
        verify(revocations).revoke("jwt-de-la-peticion", expires);
        verifyNoMoreInteractions(revocations);
    }
    @Test
    void noReportaExitoSiNoPuedePersistirRevocacion() {
        var current = mock(CurrentTokenPort.class);
        var revocations = mock(TokenRevocationPort.class);
        when(current.tokenValue()).thenReturn("jwt");
        Instant expires = Instant.now().plusSeconds(600);
        when(current.expiresAt()).thenReturn(expires);
        doThrow(new IllegalStateException("BD no disponible")).when(revocations).revoke("jwt", expires);
        assertThatThrownBy(() -> new LogoutUseCase(current, revocations).execute())
                .isInstanceOf(IllegalStateException.class);
    }
}

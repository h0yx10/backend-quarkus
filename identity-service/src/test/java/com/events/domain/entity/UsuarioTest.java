package com.events.domain.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class UsuarioTest {
    @Test
    void validaCamposYConservaPerfilAlCambiarRoles() {
        Usuario usuario = new Usuario(" Camila ", " CAMILA@correo.com ", "hash");
        var perfil = usuario.habilitarComoOrganizador();
        assertThat(usuario.habilitarComoOrganizador()).isSameAs(perfil);
        assertThat(perfil.getNombre()).isEqualTo("Camila");
        assertThat(perfil.getCorreo()).isEqualTo("camila@correo.com");
        assertThatThrownBy(() -> usuario.actualizarDatos("   ", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> usuario.actualizarDatos("a".repeat(121), null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> usuario.actualizarDatos(null, "  ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> usuario.actualizarDatos(null, "a".repeat(181))).isInstanceOf(IllegalArgumentException.class);
        usuario.actualizarDatos(null, null);
        usuario.onCreate();
        assertThat(usuario.getCreatedAt()).isNotNull();
    }
    @Test
    void passwordRespetaBytesUtf8YCaracteres() {
        PasswordPolicy.validar("a".repeat(72));
        PasswordPolicy.validar("é".repeat(36));
        PasswordPolicy.validar("😀".repeat(8));
        for (String password : new String[]{null, "        ", "corta", "a".repeat(73), "é".repeat(37), "😀".repeat(7)})
            assertThatThrownBy(() -> PasswordPolicy.validar(password)).isInstanceOf(IllegalArgumentException.class);
    }
}

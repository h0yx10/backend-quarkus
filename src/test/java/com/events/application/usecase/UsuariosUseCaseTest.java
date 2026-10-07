package com.events.application.usecase;

import com.events.application.port.out.*;
import com.events.domain.entity.*;
import com.events.domain.exception.*;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class UsuariosUseCaseTest {
    private final UsuarioRepositoryPort usuarios = mock(UsuarioRepositoryPort.class);
    private final RolRepositoryPort roles = mock(RolRepositoryPort.class);
    private final PasswordHasherPort passwords = mock(PasswordHasherPort.class);
    private final CurrentUsuarioPort current = mock(CurrentUsuarioPort.class);
    private final TransactionPort transaction = new TransactionPort() {
        public <T> T execute(Supplier<T> action) { return action.get(); }
    };
    private final UsuariosUseCase useCase = new UsuariosUseCase(usuarios, roles, passwords, current, transaction);
    private Usuario usuario;
    private static void setId(Usuario usuario) {
        try {
            var field = Usuario.class.getDeclaredField("id"); field.setAccessible(true); field.set(usuario, UUID.randomUUID());
        } catch (ReflectiveOperationException ex) { throw new AssertionError(ex); }
    }
    @BeforeEach
    void setup() {
        usuario = new Usuario("Camila", "camila@correo.com", "hash");
        setId(usuario);
        when(current.currentUsuarioId()).thenReturn(usuario.getId());
        when(usuarios.findByIdForUpdate(usuario.getId())).thenReturn(Optional.of(usuario));
        when(usuarios.save(any())).thenAnswer(i -> i.getArgument(0));
        for (NombreRol nombre : NombreRol.values())
            when(roles.findByNombre(nombre)).thenReturn(Optional.of(new Rol(nombre)));
    }
    @Test
    void creaConRolPorDefectoYPerfil() {
        when(passwords.hash("Secreta123")).thenReturn("bcrypt");
        var creado = useCase.create(" Camila ", " CAMILA@correo.com ", "Secreta123", null);
        assertThat(creado.getNombre()).isEqualTo("Camila");
        assertThat(creado.getCorreo()).isEqualTo("camila@correo.com");
        assertThat(creado.getPasswordHash()).isEqualTo("bcrypt");
        assertThat(creado.tieneRol(NombreRol.ORGANIZADOR)).isTrue();
        assertThat(creado.getOrganizador()).isNotNull();
    }
    @Test
    void creaAdminSinPerfil() {
        var creado = useCase.create("Admin", "admin@correo.com", "Secreta123", Set.of(NombreRol.ADMIN));
        assertThat(creado.getOrganizador()).isNull();
        assertThat(creado.isActivo()).isTrue();
        assertThat(creado.tieneRol(NombreRol.ADMIN)).isTrue();
    }
    @Test
    void consultaUsuarioYNoEncontrado() {
        when(usuarios.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        assertThat(useCase.get(usuario.getId())).isSameAs(usuario);
        assertThatThrownBy(() -> useCase.get(UUID.randomUUID())).isInstanceOf(UsuarioNotFoundException.class);
    }
    @Test
    void actualizaPerfilYPasswordConActual() {
        when(passwords.matches("actual", "hash")).thenReturn(true);
        when(passwords.hash("NuevaClave123")).thenReturn("nuevoHash");
        var resultado = useCase.updateCurrent("Otro", " OTRO@correo.com ", "NuevaClave123", "actual");
        assertThat(resultado.getNombre()).isEqualTo("Otro");
        assertThat(resultado.getCorreo()).isEqualTo("otro@correo.com");
        assertThat(resultado.getPasswordHash()).isEqualTo("nuevoHash");
        assertThat(resultado.getRoles()).isEmpty();
    }
    @Test
    void rechazaPasswordActualIncorrectoOAusente() {
        assertThatThrownBy(() -> useCase.updateCurrent(null, null, "NuevaClave123", null))
                .isInstanceOf(CredencialesInvalidasException.class);
        assertThatThrownBy(() -> useCase.updateCurrent(null, null, "NuevaClave123", "incorrecta"))
                .isInstanceOf(CredencialesInvalidasException.class);
        verify(usuarios, never()).save(any());
    }
    @Test
    void adminRestablecePasswordAjeno() {
        when(current.currentUsuarioId()).thenReturn(UUID.randomUUID());
        when(passwords.hash("NuevaClave123")).thenReturn("nuevoHash");
        useCase.update(usuario.getId(), null, null, "NuevaClave123", null, null, null);
        assertThat(usuario.getPasswordHash()).isEqualTo("nuevoHash");
        verify(passwords, never()).matches(any(), any());
    }
    @Test
    void asignaRolesCreaPerfilYDesactiva() {
        useCase.update(usuario.getId(), null, null, null, null, Set.of(NombreRol.ORGANIZADOR), false);
        assertThat(usuario.getOrganizador()).isNotNull();
        assertThat(usuario.isActivo()).isFalse();
        useCase.update(usuario.getId(), null, null, null, null, Set.of(NombreRol.ADMIN), true);
        assertThat(usuario.tieneRol(NombreRol.ORGANIZADOR)).isFalse();
        assertThat(usuario.getOrganizador()).isNotNull();
        assertThat(usuario.isActivo()).isTrue();
    }
    @Test
    void rechazaActividadSinPerfilYRolesVacios() {
        assertThatThrownBy(() -> useCase.update(usuario.getId(), null, null, null, null, null, false))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.create("Admin", "admin@correo.com", "Secreta123", Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
        Set<NombreRol> invalidos = new HashSet<>(); invalidos.add(null);
        assertThatThrownBy(() -> useCase.create("Admin", "admin@correo.com", "Secreta123", invalidos))
                .isInstanceOf(IllegalArgumentException.class);
    }
    @Test
    void requiereRolesSembrados() {
        when(roles.findByNombre(NombreRol.ADMIN)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.create("Admin", "admin@correo.com", "Secreta123", Set.of(NombreRol.ADMIN)))
                .isInstanceOf(IllegalStateException.class);
    }
    @Test
    void rechazaCorreoDuplicadoYPermiteCorreoPropio() {
        when(usuarios.findByCorreo("camila@correo.com")).thenReturn(Optional.of(usuario));
        assertThatThrownBy(() -> useCase.create("Otra", "CAMILA@correo.com", "Secreta123", null))
                .isInstanceOf(CorreoYaRegistradoException.class);
        useCase.updateCurrent(null, "CAMILA@correo.com", null, null);
        Usuario otra = new Usuario("Otra", "otra@correo.com", "hash");
        setId(otra);
        when(usuarios.findByCorreo("otra@correo.com")).thenReturn(Optional.of(otra));
        assertThatThrownBy(() -> useCase.updateCurrent(null, "otra@correo.com", null, null))
                .isInstanceOf(CorreoYaRegistradoException.class);
    }
    @Test
    void protegeUltimoAdminAlEliminarYRetirarRol() {
        usuario.asignarRol(new Rol(NombreRol.ADMIN));
        when(usuarios.countActiveAdmins()).thenReturn(1L);
        assertThatThrownBy(() -> useCase.delete(usuario.getId())).isInstanceOf(UsuarioConflictException.class);
        assertThatThrownBy(() -> useCase.update(usuario.getId(), null, null, null, null,
                Set.of(NombreRol.ORGANIZADOR), null)).isInstanceOf(UsuarioConflictException.class);
        verify(usuarios, never()).delete(any());
    }
    @Test
    void protegeUltimoAdminAlDesactivar() {
        usuario.asignarRol(new Rol(NombreRol.ADMIN)); usuario.habilitarComoOrganizador();
        when(usuarios.countActiveAdmins()).thenReturn(1L);
        assertThatThrownBy(() -> useCase.update(usuario.getId(), null, null, null, null, null, false))
                .isInstanceOf(UsuarioConflictException.class);
    }
    @Test
    void permiteRetirarAdminCuandoQuedaOtro() {
        usuario.asignarRol(new Rol(NombreRol.ADMIN));
        when(usuarios.countActiveAdmins()).thenReturn(2L);
        useCase.update(usuario.getId(), null, null, null, null, Set.of(NombreRol.ORGANIZADOR), null);
        assertThat(usuario.tieneRol(NombreRol.ADMIN)).isFalse();
    }
    @Test
    void eliminaCuentaPropiaSinDependencias() {
        useCase.deleteCurrent();
        verify(usuarios).delete(usuario);
        verify(roles).lockAdminGuard();
    }
    @Test
    void eliminaAdminSiQuedaOtro() {
        usuario.asignarRol(new Rol(NombreRol.ADMIN));
        when(usuarios.countActiveAdmins()).thenReturn(2L);
        useCase.delete(usuario.getId());
        verify(usuarios).delete(usuario);
    }
    @Test
    void bloqueaBajaConDatosYUsuarioInexistente() {
        when(usuarios.hasBusinessData(usuario.getId())).thenReturn(true);
        assertThatThrownBy(() -> useCase.deleteCurrent()).isInstanceOf(UsuarioConflictException.class);
        UUID inexistente = UUID.randomUUID();
        assertThatThrownBy(() -> useCase.delete(inexistente)).isInstanceOf(UsuarioNotFoundException.class);
        assertThatThrownBy(() -> useCase.update(inexistente, null, null, null, null, null, null))
                .isInstanceOf(UsuarioNotFoundException.class);
    }
    @Test
    void validaPasswordAntesDeGuardar() {
        assertThatThrownBy(() -> useCase.create("A", "a@correo.com", "corta", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.updateCurrent(null, null, "corta", "actual"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(usuarios, never()).save(any());
    }
}

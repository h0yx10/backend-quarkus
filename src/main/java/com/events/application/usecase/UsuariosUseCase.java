package com.events.application.usecase;

import com.events.application.port.in.UsuariosPort;
import com.events.application.port.out.*;
import com.events.domain.entity.*;
import com.events.domain.exception.*;
import java.util.*;

public class UsuariosUseCase implements UsuariosPort {
    private final UsuarioRepositoryPort usuarios;
    private final RolRepositoryPort roles;
    private final PasswordHasherPort passwords;
    private final CurrentUsuarioPort current;
    private final TransactionPort transaction;

    public UsuariosUseCase(UsuarioRepositoryPort usuarios, RolRepositoryPort roles, PasswordHasherPort passwords,
                           CurrentUsuarioPort current, TransactionPort transaction) {
        this.usuarios = usuarios; this.roles = roles; this.passwords = passwords;
        this.current = current; this.transaction = transaction;
    }
    @Override
    public Usuario get(UUID id) {
        return usuarios.findById(id).orElseThrow(() -> new UsuarioNotFoundException("No encontramos el usuario."));
    }
    @Override
    public Usuario create(String nombre, String correo, String password, Set<NombreRol> nombres) {
        PasswordPolicy.validar(password);
        return transaction.execute(() -> {
            String email = Usuario.normalizarCorreo(correo);
            verificarCorreo(email, null);
            Usuario usuario = new Usuario(nombre, email, passwords.hash(password));
            aplicarRoles(usuario, nombres == null ? Set.of(NombreRol.ORGANIZADOR) : nombres);
            return usuarios.save(usuario);
        });
    }
    @Override
    public Usuario update(UUID id, String nombre, String correo, String password, String passwordActual,
                          Set<NombreRol> nombres, Boolean activo) {
        return actualizar(id, nombre, correo, password, passwordActual, nombres, activo);
    }
    @Override
    public Usuario updateCurrent(String nombre, String correo, String password, String passwordActual) {
        return actualizar(current.currentUsuarioId(), nombre, correo, password, passwordActual, null, null);
    }
    private Usuario actualizar(UUID id, String nombre, String correo, String password, String passwordActual,
                               Set<NombreRol> nombres, Boolean activo) {
        return transaction.execute(() -> {
            // Serializa cambios y bajas para proteger al ultimo ADMIN incluso con peticiones concurrentes.
            roles.lockAdminGuard();
            Usuario usuario = bloqueado(id);
            boolean eraAdmin = adminHabilitado(usuario);
            long adminsAntes = eraAdmin ? usuarios.countActiveAdmins() : 0;
            if (correo != null) verificarCorreo(Usuario.normalizarCorreo(correo), id);
            if (password != null) {
                PasswordPolicy.validar(password);
                if (id.equals(current.currentUsuarioId()) && (passwordActual == null
                        || !passwords.matches(passwordActual, usuario.getPasswordHash())))
                    throw new CredencialesInvalidasException("La contrasena actual es incorrecta.");
                usuario.cambiarPasswordHash(passwords.hash(password));
            }
            usuario.actualizarDatos(nombre, correo);
            if (nombres != null) aplicarRoles(usuario, nombres);
            if (activo != null) {
                if (usuario.getOrganizador() == null)
                    throw new IllegalArgumentException("El usuario no tiene perfil de organizador.");
                usuario.getOrganizador().cambiarActivo(activo);
            }
            if (eraAdmin && !adminHabilitado(usuario)) protegerUltimoAdmin(adminsAntes);
            return usuarios.save(usuario);
        });
    }
    @Override
    public void delete(UUID id) {
        transaction.execute(() -> {
            roles.lockAdminGuard();
            Usuario usuario = bloqueado(id);
            if (adminHabilitado(usuario)) protegerUltimoAdmin(usuarios.countActiveAdmins());
            if (usuarios.hasBusinessData(id))
                throw new UsuarioConflictException("No se puede eliminar un usuario con eventos o capacidades asociados.");
            usuarios.delete(usuario);
            return null;
        });
    }
    @Override
    public void deleteCurrent() { delete(current.currentUsuarioId()); }
    private Usuario bloqueado(UUID id) {
        return usuarios.findByIdForUpdate(id)
                .orElseThrow(() -> new UsuarioNotFoundException("No encontramos el usuario."));
    }
    private void verificarCorreo(String correo, UUID id) {
        if (usuarios.findByCorreo(correo).filter(u -> !u.getId().equals(id)).isPresent())
            throw new CorreoYaRegistradoException("Ya existe una cuenta con ese correo.");
    }
    private void aplicarRoles(Usuario usuario, Set<NombreRol> nombres) {
        if (nombres.isEmpty() || nombres.stream().anyMatch(Objects::isNull))
            throw new IllegalArgumentException("Debes asignar al menos un rol valido.");
        Set<Rol> entidades = new HashSet<>();
        for (NombreRol nombre : nombres) entidades.add(roles.findByNombre(nombre)
                .orElseThrow(() -> new IllegalStateException("Ejecuta docs/schema.sql: falta el rol " + nombre)));
        usuario.reemplazarRoles(entidades);
        if (nombres.contains(NombreRol.ORGANIZADOR)) usuario.habilitarComoOrganizador();
    }
    private boolean adminHabilitado(Usuario usuario) {
        return usuario.puedeIniciarSesion() && usuario.tieneRol(NombreRol.ADMIN);
    }
    private void protegerUltimoAdmin(long adminsAntes) {
        if (adminsAntes <= 1)
            throw new UsuarioConflictException("No se puede eliminar, desactivar ni retirar el rol del ultimo ADMIN habilitado.");
    }
}

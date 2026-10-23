package com.events.application.port.in;

import com.events.domain.entity.NombreRol;
import com.events.domain.entity.Usuario;
import java.util.Set;
import java.util.UUID;

public interface UsuariosPort {
    Usuario get(UUID id);
    Usuario create(String nombre, String correo, String password, Set<NombreRol> roles);
    Usuario update(UUID id, String nombre, String correo, String password, String passwordActual,
                   Set<NombreRol> roles, Boolean activo);
    void delete(UUID id);
    Usuario updateCurrent(String nombre, String correo, String password, String passwordActual);
    void deleteCurrent();
}

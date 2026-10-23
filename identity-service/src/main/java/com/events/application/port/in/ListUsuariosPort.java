package com.events.application.port.in;

import com.events.domain.entity.Usuario;
import java.util.List;

/**
 * Lista todos los usuarios registrados. Solo accesible con rol ADMIN.
 */
public interface ListUsuariosPort {
    List<Usuario> execute();
}

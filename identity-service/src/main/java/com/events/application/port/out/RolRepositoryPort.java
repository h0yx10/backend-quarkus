package com.events.application.port.out;

import com.events.domain.entity.NombreRol;
import com.events.domain.entity.Rol;
import java.util.Optional;

public interface RolRepositoryPort {
    void lockAdminGuard();
    Optional<Rol> findByNombre(NombreRol nombre);

    Rol save(Rol rol);
}

package com.events.application.port.in;

import com.events.domain.entity.CapacidadDiaria;

public interface GetCapacidadPort {
    /**
     * Devuelve la capacidad vigente del organizador demo, o null si nunca se ha configurado
     * (en ese caso el llamador debe asumir el valor por defecto de 6h).
     */
    CapacidadDiaria execute();
}

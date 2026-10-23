package com.events.application.port.in;

import com.events.domain.entity.CapacidadDiaria;
import java.math.BigDecimal;

public interface UpdateCapacidadPort {
    CapacidadDiaria execute(BigDecimal limiteHoras);
}

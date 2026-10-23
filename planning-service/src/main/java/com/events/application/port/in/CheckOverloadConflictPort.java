package com.events.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Previsualiza si reprogramar una subtarea a una fecha/horas dadas causaria sobrecarga (US-07),
 * sin persistir el cambio.
 */
public interface CheckOverloadConflictPort {
    OverloadCheckResult execute(UUID subtareaId, LocalDate nuevaFecha, BigDecimal nuevasHoras);
}

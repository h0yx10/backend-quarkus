package com.events.infrastructure.adapter.in.rest.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CapacidadResponse(BigDecimal limiteHoras, boolean porDefecto, LocalDate fecha) {
}

package com.events.infrastructure.adapter.in.rest.dto;

import java.math.BigDecimal;

public record OverloadCheckResponse(
        boolean conflict,
        BigDecimal plannedHours,
        BigDecimal limitHours,
        BigDecimal exceedsBy
) {
}

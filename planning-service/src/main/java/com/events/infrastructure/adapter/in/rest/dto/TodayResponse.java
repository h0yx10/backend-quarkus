package com.events.infrastructure.adapter.in.rest.dto;

import java.util.List;

public record TodayResponse(
        List<SubtareaResponse> vencidas,
        List<SubtareaResponse> paraHoy,
        List<SubtareaResponse> proximas,
        String regla
) {
}

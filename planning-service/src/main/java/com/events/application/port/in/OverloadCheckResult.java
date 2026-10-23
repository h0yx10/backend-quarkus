package com.events.application.port.in;

import java.math.BigDecimal;

public record OverloadCheckResult(boolean conflict, BigDecimal plannedHours, BigDecimal limitHours,
                                   BigDecimal exceedsBy) {
}

package com.events.domain.exception;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.SortedMap;
import java.util.stream.Collectors;

/**
 * Se lanza al bajar el limite diario (US-12) por debajo de las horas ya planificadas en algun dia
 * desde hoy: aceptarlo dejaria esos dias en sobrecarga sin que nadie lo decidiera (US-07).
 */
public class LimiteBajoCargaPlanificadaException extends RuntimeException {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final BigDecimal limitHours;
    private final SortedMap<LocalDate, BigDecimal> diasSobrecargados;

    public LimiteBajoCargaPlanificadaException(BigDecimal limitHours, SortedMap<LocalDate, BigDecimal> diasSobrecargados) {
        super("No puedes bajar el limite a " + limitHours.stripTrailingZeros().toPlainString()
                + " h: hay dias con mas horas planificadas ("
                + diasSobrecargados.entrySet().stream()
                        .map(dia -> FECHA.format(dia.getKey()) + ": " + dia.getValue().stripTrailingZeros().toPlainString() + " h")
                        .collect(Collectors.joining(", "))
                + "). Reprograma o reduce esas gestiones primero.");
        this.limitHours = limitHours;
        this.diasSobrecargados = Collections.unmodifiableSortedMap(diasSobrecargados);
    }

    public BigDecimal getLimitHours() {
        return limitHours;
    }

    public SortedMap<LocalDate, BigDecimal> getDiasSobrecargados() {
        return diasSobrecargados;
    }
}

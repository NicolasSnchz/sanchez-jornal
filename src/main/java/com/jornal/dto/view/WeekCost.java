package com.jornal.dto.view;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Costo de mano de obra de una semana, para la grafica del panel del maestro. */
public record WeekCost(LocalDate weekStart, BigDecimal total, int percentOfMax) {
}

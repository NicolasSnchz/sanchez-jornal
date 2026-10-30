package com.jornal.dto.view;

import com.jornal.entity.WorkDayStatus;
import com.jornal.util.TimeScale;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** Jornada con sus horas y su valor ya calculados por el dominio. */
public record WorkDayRow(
        UUID id,
        LocalDate date,
        LocalTime checkInTime,
        LocalTime checkOutTime,
        int breakMinutes,
        int workedMinutes,
        int overtimeMinutes,
        BigDecimal fraction,
        BigDecimal value,
        WorkDayStatus status,
        String taskPerformed,
        String notes,
        String disputeReason,
        boolean correctedAfterDispute,
        boolean settled,
        boolean editable) {

    public boolean isHalfOrLess() {
        return fraction.compareTo(new BigDecimal("0.50")) <= 0;
    }

    /** Inicio de la barra de presencia en la escala 06:00-19:00 (porcentaje). */
    public String barStart() {
        return TimeScale.position(checkInTime);
    }

    public String barLength() {
        return TimeScale.length((int) Duration.between(checkInTime, checkOutTime).toMinutes());
    }

    /** Las horas extra se dibujan al final de la presencia: son las que pasan del horario. */
    public String overtimeStart() {
        return TimeScale.position(checkOutTime.minusMinutes(overtimeMinutes));
    }

    public String overtimeLength() {
        return TimeScale.length(overtimeMinutes);
    }
}

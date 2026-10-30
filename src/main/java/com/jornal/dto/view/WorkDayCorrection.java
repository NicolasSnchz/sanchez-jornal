package com.jornal.dto.view;

import com.jornal.dto.form.WorkDayForm;
import java.util.UUID;

/** Jornada que el maestro va a corregir: el registro actual y el formulario precargado. */
public record WorkDayCorrection(UUID workDayId, UUID assignmentId, WorkDayRow current, WorkDayForm form) {
}

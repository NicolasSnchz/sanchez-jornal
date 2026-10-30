package com.jornal.dto.form;

import com.jornal.entity.WorkDay;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

/** Registro de horas de un dia, tal como lo anota el maestro en la obra. */
@Schema(description = "Jornada de un d\u00eda")
public class WorkDayForm {

    @Schema(description = "Fecha; no puede ser futura, domingo ni festivo", example = "2026-10-29")
    @NotNull
    private LocalDate date;

    @Schema(description = "Hora de entrada", example = "07:00", type = "string")
    @NotNull
    private LocalTime checkInTime;

    @Schema(description = "Hora de salida; las horas extra se pagan con recargo del 25 %",
            example = "16:00", type = "string")
    @NotNull
    private LocalTime checkOutTime;

    @Schema(description = "Minutos de descanso que no se pagan", example = "60")
    @Min(0)
    @Max(WorkDay.MAX_BREAK_MINUTES)
    private int breakMinutes;

    @Schema(description = "Labor realizada", example = "Armado de hierro para columnas")
    @NotBlank
    @Size(max = 200)
    private String taskPerformed;

    @Schema(description = "Novedades (opcional)", example = "Lleg\u00f3 15 minutos tarde por la lluvia")
    @Size(max = 500)
    private String notes;

    public static WorkDayForm from(WorkDay day) {
        WorkDayForm form = new WorkDayForm();
        form.date = day.getDate();
        form.checkInTime = day.getCheckInTime();
        form.checkOutTime = day.getCheckOutTime();
        form.breakMinutes = day.getBreakMinutes();
        form.taskPerformed = day.getTaskPerformed();
        form.notes = day.getNotes();
        return form;
    }

    public WorkDay.TimeRecord toRecord() {
        return new WorkDay.TimeRecord(checkInTime, checkOutTime, breakMinutes, taskPerformed, notes);
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(LocalTime checkInTime) {
        this.checkInTime = checkInTime;
    }

    public LocalTime getCheckOutTime() {
        return checkOutTime;
    }

    public void setCheckOutTime(LocalTime checkOutTime) {
        this.checkOutTime = checkOutTime;
    }

    public int getBreakMinutes() {
        return breakMinutes;
    }

    public void setBreakMinutes(int breakMinutes) {
        this.breakMinutes = breakMinutes;
    }

    public String getTaskPerformed() {
        return taskPerformed;
    }

    public void setTaskPerformed(String taskPerformed) {
        this.taskPerformed = taskPerformed;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

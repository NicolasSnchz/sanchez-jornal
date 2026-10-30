package com.jornal.dto.form;

import com.jornal.entity.WorkDay;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

/** Registro de horas de un dia, tal como lo anota el maestro en la obra. */
public class WorkDayForm {

    @NotNull
    private LocalDate date;

    @NotNull
    private LocalTime checkInTime;

    @NotNull
    private LocalTime checkOutTime;

    @Min(0)
    @Max(WorkDay.MAX_BREAK_MINUTES)
    private int breakMinutes;

    @NotBlank
    @Size(max = 200)
    private String taskPerformed;

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

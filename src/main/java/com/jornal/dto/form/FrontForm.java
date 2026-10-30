package com.jornal.dto.form;

import com.jornal.entity.Trade;
import com.jornal.entity.WorkFront;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;

/** Datos para abrir un frente de trabajo. Los valores por defecto son una jornada tipica de obra. */
public class FrontForm {

    @NotBlank
    @Size(max = 100)
    private String task;

    @NotNull
    private Trade requiredTrade;

    @Min(1)
    @Max(WorkFront.MAX_OPENINGS)
    private int openings = 1;

    /** Jornal en pesos enteros. El tope evita errores de digitacion (un cero de mas). */
    @NotNull
    @Min(10_000)
    @Max(1_000_000)
    private BigDecimal offeredDailyWage;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    @NotEmpty
    private Set<DayOfWeek> workDays = EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);

    @NotNull
    private LocalTime startTime = LocalTime.of(7, 0);

    @NotNull
    private LocalTime endTime = LocalTime.of(16, 0);

    @Min(0)
    @Max(WorkFront.MAX_LUNCH_MINUTES)
    private int lunchBreakMinutes = 60;

    public WorkFront.FrontData toData() {
        return new WorkFront.FrontData(task, requiredTrade, openings, offeredDailyWage, startDate, endDate,
                workDays, startTime, endTime, lunchBreakMinutes);
    }

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public Trade getRequiredTrade() {
        return requiredTrade;
    }

    public void setRequiredTrade(Trade requiredTrade) {
        this.requiredTrade = requiredTrade;
    }

    public int getOpenings() {
        return openings;
    }

    public void setOpenings(int openings) {
        this.openings = openings;
    }

    public BigDecimal getOfferedDailyWage() {
        return offeredDailyWage;
    }

    public void setOfferedDailyWage(BigDecimal offeredDailyWage) {
        this.offeredDailyWage = offeredDailyWage;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Set<DayOfWeek> getWorkDays() {
        return workDays;
    }

    public void setWorkDays(Set<DayOfWeek> workDays) {
        this.workDays = workDays;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public int getLunchBreakMinutes() {
        return lunchBreakMinutes;
    }

    public void setLunchBreakMinutes(int lunchBreakMinutes) {
        this.lunchBreakMinutes = lunchBreakMinutes;
    }
}

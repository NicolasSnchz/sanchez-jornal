package com.jornal.support;

import com.jornal.entity.Assignment;
import com.jornal.entity.ConstructionSite;
import com.jornal.entity.DocumentType;
import com.jornal.entity.Foreman;
import com.jornal.entity.Gender;
import com.jornal.entity.Trade;
import com.jornal.entity.User;
import com.jornal.entity.WorkDay;
import com.jornal.entity.WorkFront;
import com.jornal.entity.Worker;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Objetos de dominio listos para las pruebas. Las fechas giran alrededor de un
 * "hoy" fijo (jueves 29 de octubre de 2026) para que las pruebas no dependan
 * del calendario real.
 */
public final class TestData {

    /** Jueves. */
    public static final LocalDate TODAY = LocalDate.of(2026, 10, 29);
    /** Lunes de la semana de TODAY. */
    public static final LocalDate MONDAY = LocalDate.of(2026, 10, 26);
    public static final LocalDateTime NOW = TODAY.atTime(10, 0);
    public static final BigDecimal WAGE = new BigDecimal("80000");
    /** 07:00 a 16:00 con 60 min de almuerzo = 480 min programados. */
    public static final LocalTime START = LocalTime.of(7, 0);
    public static final LocalTime END = LocalTime.of(16, 0);

    private static final AtomicInteger SEQUENCE = new AtomicInteger(1000);

    private TestData() {
    }

    public static User.PersonalData person(String firstName, String lastName, LocalDate birthDate) {
        int n = SEQUENCE.incrementAndGet();
        return new User.PersonalData(DocumentType.CC, "10" + n, firstName, lastName, birthDate, Gender.MALE,
                "3001234567", firstName.toLowerCase().replace(' ', '.') + n + "@prueba.co", "C\u00facuta");
    }

    public static Foreman foreman() {
        return new Foreman(person("Carlos", "Rincon", LocalDate.of(1980, 1, 1)), "Rincon SAS", "{hash}", NOW);
    }

    public static Worker worker() {
        return worker(Trade.HELPER, TODAY.plusYears(1));
    }

    public static Worker worker(Trade trade, LocalDate arlValidUntil) {
        return new Worker(person("Yeison", "Quintero", LocalDate.of(1998, 6, 21)),
                new Worker.WorkProfile(trade, 3, "La Libertad", false, arlValidUntil, "3009998888"), "{hash}", NOW);
    }

    public static Worker workerBornOn(LocalDate birthDate) {
        return new Worker(person("Menor", "Edad", birthDate),
                new Worker.WorkProfile(Trade.HELPER, 0, "Centro", false, TODAY.plusYears(1), "3009998888"),
                "{hash}", NOW);
    }

    /** Obra en curso del 5 de octubre al 31 de diciembre de 2026. */
    public static ConstructionSite siteInProgress(Foreman foreman) {
        ConstructionSite site = sitePlanned(foreman);
        site.start();
        return site;
    }

    public static ConstructionSite sitePlanned(Foreman foreman) {
        return new ConstructionSite(foreman, new ConstructionSite.SiteData("Casa Duarte", null, "Calle 12 # 2E-45",
                "Caobos", "C\u00facuta", null, LocalDate.of(2026, 10, 5), LocalDate.of(2026, 12, 31)), NOW);
    }

    public static WorkFront.FrontData frontData(int openings) {
        return new WorkFront.FrontData("Fundida de placa", Trade.HELPER, openings, WAGE, LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 12, 18), EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), START, END, 60);
    }

    public static WorkFront front(ConstructionSite site) {
        return site.openFront(frontData(3));
    }

    /** Asignacion aceptada que empieza el lunes 5 de octubre. */
    public static Assignment assignmentInProgress() {
        WorkFront front = front(siteInProgress(foreman()));
        Assignment assignment = Assignment.invite(front, worker(), WAGE, LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 10, 2).atTime(17, 0));
        assignment.accept(LocalDate.of(2026, 10, 2).atTime(19, 0));
        return assignment;
    }

    public static WorkDay.TimeRecord fullDay() {
        return new WorkDay.TimeRecord(START, END, 60, "Fundida", null);
    }

    public static WorkDay.TimeRecord record(String checkIn, String checkOut, int breakMinutes) {
        return new WorkDay.TimeRecord(LocalTime.parse(checkIn), LocalTime.parse(checkOut), breakMinutes, "Labor", null);
    }

    /** Registra y confirma una jornada completa ese dia. */
    public static WorkDay confirmedDay(Assignment assignment, LocalDate date) {
        WorkDay day = assignment.registerWorkDay(date, fullDay(), date, date.atTime(16, 30));
        day.confirm(date.atTime(18, 0));
        return day;
    }
}

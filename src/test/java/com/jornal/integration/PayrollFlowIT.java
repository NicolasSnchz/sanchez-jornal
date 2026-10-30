package com.jornal.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jornal.dto.form.CompletionForm;
import com.jornal.dto.form.ForemanRegistrationForm;
import com.jornal.dto.form.FrontForm;
import com.jornal.dto.form.InvitationForm;
import com.jornal.dto.form.PaymentForm;
import com.jornal.dto.form.PersonalDataForm;
import com.jornal.dto.form.SiteForm;
import com.jornal.dto.form.WorkDayForm;
import com.jornal.dto.form.WorkerRegistrationForm;
import com.jornal.dto.view.AssignmentDetail;
import com.jornal.dto.view.CandidateRow;
import com.jornal.dto.view.PaymentRow;
import com.jornal.dto.view.PaymentsMonth;
import com.jornal.dto.view.WorkDayRow;
import com.jornal.entity.AssignmentStatus;
import com.jornal.entity.DocumentType;
import com.jornal.entity.Gender;
import com.jornal.entity.PaymentMethod;
import com.jornal.entity.PaymentType;
import com.jornal.entity.Trade;
import com.jornal.entity.WorkDayStatus;
import com.jornal.repository.UserRepository;
import com.jornal.security.JornalUserDetails;
import com.jornal.service.AssignmentService;
import com.jornal.service.DashboardService;
import com.jornal.service.PaymentService;
import com.jornal.service.RegistrationService;
import com.jornal.service.SiteAction;
import com.jornal.service.SiteService;
import com.jornal.service.WorkDayService;
import com.jornal.support.IntegrationTest;
import com.jornal.support.MutableClock;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Recorre el caso de uso completo de Jornal con los servicios reales, la base de
 * datos y la seguridad por metodo, adelantando el reloj dia a dia:
 * registro, obra, frente, invitacion, jornadas, objecion y correccion,
 * anticipo, liquidacion de periodo, liquidacion final y calificaciones.
 */
@IntegrationTest
class PayrollFlowIT {

    /** Semana sin festivos (el lunes 16 de noviembre si es festivo: Independencia de Cartagena). */
    private static final LocalDate MON = LocalDate.of(2026, 11, 23);

    @Autowired
    private RegistrationService registrationService;
    @Autowired
    private SiteService siteService;
    @Autowired
    private AssignmentService assignmentService;
    @Autowired
    private WorkDayService workDayService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private DashboardService dashboardService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MutableClock clock;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void cicloCompletoDeUnaAsignacion_cuadraAlPeso() {
        clock.setTo(LocalDateTime.of(2026, 10, 29, 10, 0));

        // 1. Registro de las dos cuentas
        UUID foremanId = registrationService.registerForeman(foremanForm());
        UUID workerId = registrationService.registerWorker(workerForm());

        // 2. El maestro crea la obra, abre un frente y la inicia
        as(foremanId);
        UUID siteId = siteService.createSite(foremanId, siteForm());
        UUID frontId = siteService.openFront(foremanId, siteId, frontForm());
        siteService.changeStatus(foremanId, siteId, SiteAction.START);

        // 3. Busca al trabajador y lo invita
        List<CandidateRow> candidates = assignmentService.findCandidates(foremanId, frontId, "yeison");
        assertThat(candidates).extracting(CandidateRow::workerId).containsExactly(workerId);
        assertThat(candidates.get(0).eligible()).isTrue();
        UUID assignmentId = assignmentService.invite(foremanId, frontId, invitation(workerId));

        // 4. El trabajador acepta
        as(workerId);
        assignmentService.respond(workerId, assignmentId, true);

        // 5. Lunes completo, martes con 2 h extra, miercoles media jornada (lluvia)
        as(foremanId);
        at(MON, 17, 0);
        workDayService.registerWorkDay(foremanId, assignmentId, day(MON, "07:00", "16:00", 60));
        at(MON.plusDays(1), 18, 30);
        workDayService.registerWorkDay(foremanId, assignmentId, day(MON.plusDays(1), "07:00", "18:00", 60));
        at(MON.plusDays(2), 12, 0);
        workDayService.registerWorkDay(foremanId, assignmentId, day(MON.plusDays(2), "07:00", "11:00", 0));

        // 6. El trabajador confirma las tres; el jueves objeta la hora de salida
        as(workerId);
        assertThat(workDayService.confirmAllPending(workerId, assignmentId)).isEqualTo(3);
        as(foremanId);
        at(MON.plusDays(3), 12, 0);
        UUID thursday = workDayService.registerWorkDay(foremanId, assignmentId,
                day(MON.plusDays(3), "07:00", "11:00", 0));
        as(workerId);
        workDayService.dispute(workerId, thursday, "Trabaje hasta las 12 del dia");

        // 7. El maestro corrige y el trabajador confirma
        as(foremanId);
        workDayService.correctWorkDay(foremanId, thursday, day(MON.plusDays(3), "07:00", "12:00", 0));
        as(workerId);
        workDayService.confirm(workerId, thursday);

        AssignmentDetail detail = assignmentService.getForWorker(workerId, assignmentId, MON);
        // 80.000 + 105.000 (2 h extra) + 40.000 (4 h) + 50.000 (5 h) = 275.000
        assertThat(detail.earnedConfirmed()).isEqualByComparingTo("275000");
        assertThat(detail.workDays()).extracting(WorkDayRow::status).containsOnly(WorkDayStatus.CONFIRMED);
        assertThat(detail.workDays()).filteredOn(WorkDayRow::correctedAfterDispute).hasSize(1);

        // 8. Anticipo y liquidacion del periodo lunes-jueves
        as(foremanId);
        at(MON.plusDays(3), 18, 0);
        PaymentRow advance = paymentService.register(foremanId, assignmentId,
                payment(PaymentType.ADVANCE, "60000", null, "Transporte de la semana"));
        PaymentForm settlement = payment(PaymentType.SETTLEMENT, null, MON.plusDays(3), null);
        assertThat(paymentService.quote(foremanId, assignmentId, settlement).amount()).isEqualByComparingTo("215000");
        PaymentRow settled = paymentService.register(foremanId, assignmentId, settlement);
        assertThat(settled.receiptNumber()).matches("JOR-\\d{6}");

        as(workerId);
        paymentService.confirmReceipt(workerId, advance.id());
        paymentService.confirmReceipt(workerId, settled.id());

        // 9. El periodo liquidado queda cerrado: el siguiente empieza el viernes
        as(foremanId);
        AssignmentDetail afterSettlement = assignmentService.getForForeman(foremanId, assignmentId, null);
        assertThat(afterSettlement.lastSettledDate()).isEqualTo(MON.plusDays(3));
        assertThat(afterSettlement.nextUnsettledDate()).isEqualTo(MON.plusDays(4));
        assertThat(afterSettlement.balance()).isZero();

        // 10. Viernes, liquidacion final, cierre y calificaciones
        at(MON.plusDays(4), 17, 0);
        workDayService.registerWorkDay(foremanId, assignmentId, day(MON.plusDays(4), "07:00", "16:00", 60));
        as(workerId);
        workDayService.confirmAllPending(workerId, assignmentId);
        as(foremanId);
        PaymentRow finalPayment = paymentService.register(foremanId, assignmentId,
                payment(PaymentType.FINAL_SETTLEMENT, null, null, null));
        assertThat(finalPayment.amount()).isEqualByComparingTo("80000");
        assertThat(paymentService.receiptForForeman(foremanId, finalPayment.id()).amountInWords())
                .isEqualTo("ochenta mil pesos");
        assertThatThrownBy(() -> workDayService.registerWorkDay(foremanId, assignmentId,
                day(MON.plusDays(4), "07:00", "16:00", 60)))
                .extracting("code").isEqualTo("jornada.asignacion.liquidada");

        CompletionForm completion = new CompletionForm();
        completion.setRating(5);
        completion.setComment("Cumplido y ordenado");
        assignmentService.complete(foremanId, assignmentId, completion);
        siteService.changeStatus(foremanId, siteId, SiteAction.FINISH);

        as(workerId);
        assignmentService.rateForeman(workerId, assignmentId, 4);

        // 11. Todo cuadra: lo devengado es exactamente lo pagado
        AssignmentDetail closed = assignmentService.getForWorker(workerId, assignmentId, null);
        assertThat(closed.status()).isEqualTo(AssignmentStatus.COMPLETED);
        assertThat(closed.balance()).isZero();
        assertThat(closed.paidTotal()).isEqualByComparingTo(closed.earnedConfirmed()).isEqualByComparingTo("355000");
        assertThat(dashboardService.workerDashboard(workerId).completedAssignments()).isEqualTo(1);

        as(foremanId);
        PaymentsMonth november = paymentService.listMonth(foremanId, YearMonth.of(2026, 11));
        assertThat(november.total()).isEqualByComparingTo("355000");
        assertThat(november.advancesTotal()).isEqualByComparingTo("60000");
        assertThat(november.settlementsCount()).isEqualTo(2);
    }

    // ================================================================== ayudas

    private void as(UUID userId) {
        JornalUserDetails user = JornalUserDetails.from(userRepository.findById(userId).orElseThrow(),
                LocalDateTime.now(clock));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
    }

    private void at(LocalDate date, int hour, int minute) {
        clock.setTo(date.atTime(hour, minute));
    }

    private static ForemanRegistrationForm foremanForm() {
        ForemanRegistrationForm form = person(new ForemanRegistrationForm(), "70111222", "Carlos", "maestro.flujo@prueba.co");
        form.setCompanyName("Rincon Construcciones");
        return form;
    }

    private static WorkerRegistrationForm workerForm() {
        WorkerRegistrationForm form = person(new WorkerRegistrationForm(), "1090333444", "Yeison", "yeison.flujo@prueba.co");
        form.setTrade(Trade.HELPER);
        form.setYearsOfExperience(3);
        form.setNeighborhood("La Libertad");
        form.setArlValidUntil(LocalDate.of(2027, 6, 30));
        form.setEmergencyPhone("3125550190");
        return form;
    }

    private static <T extends PersonalDataForm> T person(T form, String document, String name, String email) {
        form.setDocumentType(DocumentType.CC);
        form.setDocumentNumber(document);
        form.setFirstName(name);
        form.setLastName("Prueba");
        form.setBirthDate(LocalDate.of(1990, 5, 5));
        form.setGender(Gender.MALE);
        form.setPhone("3001234567");
        form.setEmail(email);
        form.setCity("C\u00facuta");
        form.setPassword("Clave2026segura");
        form.setPasswordConfirmation("Clave2026segura");
        form.setAcceptsDataPolicy(true);
        return form;
    }

    private static SiteForm siteForm() {
        SiteForm form = new SiteForm();
        form.setName("Casa Duarte");
        form.setAddress("Calle 12 # 2E-45");
        form.setNeighborhood("Caobos");
        form.setCity("C\u00facuta");
        form.setStartDate(MON);
        form.setEstimatedEndDate(LocalDate.of(2026, 12, 31));
        return form;
    }

    private static FrontForm frontForm() {
        FrontForm form = new FrontForm();
        form.setTask("Fundida de placa");
        form.setRequiredTrade(Trade.HELPER);
        form.setOpenings(2);
        form.setOfferedDailyWage(new BigDecimal("80000"));
        form.setStartDate(MON);
        form.setEndDate(LocalDate.of(2026, 12, 18));
        return form;
    }

    private static InvitationForm invitation(UUID workerId) {
        InvitationForm form = new InvitationForm();
        form.setWorkerId(workerId);
        form.setDailyWage(new BigDecimal("80000"));
        form.setStartDate(MON);
        return form;
    }

    private static WorkDayForm day(LocalDate date, String in, String out, int breakMinutes) {
        WorkDayForm form = new WorkDayForm();
        form.setDate(date);
        form.setCheckInTime(LocalTime.parse(in));
        form.setCheckOutTime(LocalTime.parse(out));
        form.setBreakMinutes(breakMinutes);
        form.setTaskPerformed("Fundida");
        return form;
    }

    private static PaymentForm payment(PaymentType type, String amount, LocalDate periodEnd, String reason) {
        PaymentForm form = new PaymentForm();
        form.setType(type);
        form.setAmount(amount == null ? null : new BigDecimal(amount));
        form.setMethod(PaymentMethod.CASH);
        form.setPeriodEnd(periodEnd);
        form.setReason(reason);
        form.setRequestedByWorker(reason != null);
        return form;
    }
}

package com.jornal.config;

import com.jornal.entity.Assignment;
import com.jornal.entity.ConstructionSite;
import com.jornal.entity.DocumentType;
import com.jornal.entity.Foreman;
import com.jornal.entity.Gender;
import com.jornal.entity.Payment;
import com.jornal.entity.PaymentMethod;
import com.jornal.entity.PaymentType;
import com.jornal.entity.Trade;
import com.jornal.entity.User;
import com.jornal.entity.WorkDay;
import com.jornal.entity.WorkFront;
import com.jornal.entity.Worker;
import com.jornal.repository.AssignmentRepository;
import com.jornal.repository.ConstructionSiteRepository;
import com.jornal.repository.PaymentRepository;
import com.jornal.repository.UserRepository;
import com.jornal.strategy.PaymentCommand;
import com.jornal.strategy.PaymentStrategy;
import com.jornal.util.ColombianHolidays;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Datos de demostracion, todos relativos a la fecha de hoy: la demo se ve "viva"
 * el dia que se ejecute. Se activa con app.seed.enabled=true, que encienden los
 * perfiles dev y demo; el perfil prod lo deja apagado.
 *
 * <p>Solo carga datos si la base esta vacia, asi que reiniciar el contenedor con
 * PostgreSQL no duplica nada.
 *
 * <p>No inserta filas a mano: simula el paso del tiempo llamando a los mismos
 * metodos de dominio y estrategias de pago que usa la aplicacion (invitar,
 * aceptar, registrar jornadas, confirmar, liquidar). Asi los datos de ejemplo
 * cumplen exactamente las mismas reglas que los datos reales.
 *
 * <p>Usuarios (clave para todos: Jornal2026*):
 * maestro@jornal.co, maestra2@jornal.co, trabajador@jornal.co y los demas trabajadores.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    public static final String DEMO_PASSWORD = "Jornal2026*";
    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String CITY = "C\u00facuta";
    private static final LocalTime START = LocalTime.of(7, 0);
    private static final LocalTime END = LocalTime.of(16, 0);
    private static final int LUNCH = 60;

    private final UserRepository userRepository;
    private final ConstructionSiteRepository siteRepository;
    private final AssignmentRepository assignmentRepository;
    private final PaymentRepository paymentRepository;
    private final Map<PaymentType, PaymentStrategy> strategies = new EnumMap<>(PaymentType.class);
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public DemoDataSeeder(UserRepository userRepository, ConstructionSiteRepository siteRepository,
                          AssignmentRepository assignmentRepository, PaymentRepository paymentRepository,
                          List<PaymentStrategy> strategies, PasswordEncoder passwordEncoder, Clock clock) {
        this.userRepository = userRepository;
        this.siteRepository = siteRepository;
        this.assignmentRepository = assignmentRepository;
        this.paymentRepository = paymentRepository;
        strategies.forEach(strategy -> this.strategies.put(strategy.type(), strategy));
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            log.info("Datos de demostracion omitidos: la base ya tiene usuarios");
            return;
        }
        LocalDate today = LocalDate.now(clock);
        // W1 = lunes de hace cuatro semanas; W5 = semana actual.
        LocalDate w1 = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(4);
        String hash = passwordEncoder.encode(DEMO_PASSWORD);

        // ------------------------------------------------------------ usuarios
        Foreman carlos = save(new Foreman(person(DocumentType.CC, "88234567", "Carlos Andr\u00e9s", "Rinc\u00f3n P\u00e1bon",
                LocalDate.of(1979, 3, 14), Gender.MALE, "3157894561", "maestro@jornal.co"),
                "Rinc\u00f3n Construcciones", hash, w1.minusWeeks(16).atTime(9, 0)));
        Foreman luz = save(new Foreman(person(DocumentType.CC, "60345128", "Luz Marina", "Ortega Celis",
                LocalDate.of(1983, 9, 2), Gender.FEMALE, "3204417785", "maestra2@jornal.co"),
                null, hash, w1.minusWeeks(10).atTime(10, 30)));

        Worker yeison = save(worker("1090456789", "Yeison Fabi\u00e1n", "Quintero Rojas", LocalDate.of(1998, 6, 21),
                Gender.MALE, "3112345678", "trabajador@jornal.co", Trade.HELPER, 4, "La Libertad", false,
                today.plusDays(120), "3125550190", w1.minusWeeks(8)));
        Worker maria = save(worker("1093765432", "Mar\u00eda Fernanda", "Sep\u00falveda Mora", LocalDate.of(1995, 11, 3),
                Gender.FEMALE, "3168754321", "maria.sepulveda@jornal.co", Trade.HELPER, 3, "San Luis", true,
                today.plusDays(180), "3176660021", w1.minusWeeks(6)));
        Worker wilmer = save(worker("88190221", "Wilmer Alonso", "Jaimes Pe\u00f1aranda", LocalDate.of(1985, 1, 30),
                Gender.MALE, "3013456789", "wilmer.jaimes@jornal.co", Trade.MASON, 12, "Atalaya", true,
                today.plusDays(200), "3014440088", w1.minusWeeks(20)));
        Worker deiver = save(worker("1004987123", "Deiver Stiven", "Carrillo Le\u00f3n", LocalDate.of(2003, 4, 9),
                Gender.MALE, "3229871234", "deiver.carrillo@jornal.co", Trade.HELPER, 2, "El Salado", false,
                today.plusDays(90), "3221110345", w1.minusWeeks(3)));
        // ARL vence antes de que termine el frente: aparece como candidato no apto.
        save(worker("13456098", "Jhon Jairo", "P\u00e1bon Guerrero", LocalDate.of(1976, 8, 17),
                Gender.MALE, "3105678901", "jhon.pabon@jornal.co", Trade.HELPER, 7, "Motilones", true,
                today.plusDays(10), "3106667788", w1.minusWeeks(12)));
        save(worker("1092345870", "Brayan Esneider", "Contreras D\u00edaz", LocalDate.of(2001, 12, 5),
                Gender.MALE, "3187654320", "brayan.contreras@jornal.co", Trade.HELPER, 1, "Comuneros", false,
                today.plusDays(150), "3189990011", w1.minusWeeks(2)));
        Worker edinson = save(worker("88267345", "Edinson Ferney", "Vera Su\u00e1rez", LocalDate.of(1988, 5, 26),
                Gender.MALE, "3145678123", "edinson.vera@jornal.co", Trade.ELECTRICIAN, 9, "Prados del Este", true,
                today.plusDays(60), "3147770056", w1.minusWeeks(14)));

        // ------------------------------------------------------------ obra terminada (historial)
        LocalDate oldStart = w1.minusWeeks(12);
        ConstructionSite kitchen = new ConstructionSite(carlos, new ConstructionSite.SiteData(
                "Remodelaci\u00f3n de cocina y ba\u00f1o",
                "Cambio de enchapes, mes\u00f3n en concreto y redes hidr\u00e1ulicas de un apartamento.",
                "Calle 16 # 14E-30, Conjunto Bosque Real, apto 502", "La Riviera", CITY, null,
                oldStart, oldStart.plusWeeks(4).minusDays(3)), oldStart.minusDays(10).atTime(8, 0));
        WorkFront tiling = kitchen.openFront(front("Enchape de cocina y ba\u00f1o", Trade.MASON, 1, 115_000,
                oldStart, oldStart.plusWeeks(3).minusDays(3)));
        siteRepository.save(kitchen);
        kitchen.start();
        Assignment wilmerOld = invite(tiling, wilmer, 115_000, oldStart);
        workRange(wilmerOld, oldStart, oldStart.plusWeeks(3).minusDays(3), true);
        LocalDate oldEnd = oldStart.plusWeeks(3).minusDays(2);
        pay(PaymentType.SETTLEMENT, wilmerOld, null, PaymentMethod.BANK_TRANSFER, "BCO-77120934", null, false,
                oldStart.plusWeeks(1).minusDays(3), oldStart.plusWeeks(1).minusDays(2).atTime(11, 0), true);
        pay(PaymentType.FINAL_SETTLEMENT, wilmerOld, null, PaymentMethod.BANK_TRANSFER, "BCO-77188410", null, false,
                null, oldEnd.atTime(11, 0), true);
        wilmerOld.complete(5, "Muy buen acabado; dej\u00f3 todo nivelado y limpio.", oldEnd);
        wilmerOld.rateForeman(5);
        kitchen.finish();

        // ------------------------------------------------------------ obra en curso (principal)
        ConstructionSite house = new ConstructionSite(carlos, new ConstructionSite.SiteData(
                "Casa Duarte: ampliaci\u00f3n segundo piso",
                "Construcci\u00f3n del segundo piso: columnas, placa aligerada, muros en bloque y cubierta.",
                "Calle 12 # 2E-45", "Caobos", CITY, "54001-1-26-0112",
                w1, w1.plusWeeks(11).minusDays(3)), w1.minusDays(12).atTime(7, 30));
        WorkFront slab = house.openFront(front("Fundida de columnas y placa", Trade.HELPER, 4, 75_000,
                w1, w1.plusWeeks(10).minusDays(3)));
        WorkFront masonry = house.openFront(front("Pega de bloque y repello", Trade.MASON, 2, 120_000,
                w1.plusWeeks(1), w1.plusWeeks(10).minusDays(3)));
        WorkFront wiring = house.openFront(front("Acometida e instalaci\u00f3n el\u00e9ctrica", Trade.ELECTRICIAN, 1,
                130_000, w1, w1.plusWeeks(2).minusDays(3)));
        siteRepository.save(house);
        house.start();

        // Edinson: frente terminado, liquidacion final y calificaciones.
        Assignment edinsonA = invite(wiring, edinson, 130_000, w1);
        workRange(edinsonA, w1, w1.plusWeeks(2).minusDays(3), true, w1.plusDays(8));
        overtimeDay(edinsonA, w1.plusDays(8), LocalTime.of(17, 0), "Pruebas del tablero y marcaci\u00f3n de circuitos");
        LocalDate w2Saturday = w1.plusWeeks(1).plusDays(5);
        pay(PaymentType.FINAL_SETTLEMENT, edinsonA, null, PaymentMethod.BANK_TRANSFER, "BCO-80345521", null, false,
                null, w2Saturday.atTime(9, 30), true);
        edinsonA.complete(5, "Trabajo limpio y ordenado; dej\u00f3 todos los circuitos probados.", w2Saturday);
        edinsonA.rateForeman(4);

        // Yeison: la asignacion mas completa (liquidaciones, anticipos, objecion y pendientes).
        Assignment yeisonA = invite(slab, yeison, 75_000, w1);
        LocalDate w2Wednesday = w1.plusWeeks(1).plusDays(2);
        workRange(yeisonA, w1, w1.plusWeeks(1).plusDays(4), true, w2Wednesday);
        overtimeDay(yeisonA, w2Wednesday, LocalTime.of(18, 0), "Fundida de placa, se extendi\u00f3 la jornada");
        pay(PaymentType.SETTLEMENT, yeisonA, null, PaymentMethod.CASH, null, null, false,
                w1.plusWeeks(1).plusDays(4), w2Saturday.atTime(10, 0), true);

        LocalDate w3 = w1.plusWeeks(2);
        work(yeisonA, w3, START, END, LUNCH, slab.getTask(), null, true);
        // martes de W3: no vino (no se registra nada)
        work(yeisonA, w3.plusDays(2), START, END, LUNCH, slab.getTask(), null, true);
        work(yeisonA, w3.plusDays(3), START, LocalTime.of(11, 0), 0, "Armado de formaleta",
                "Lluvia: se suspendi\u00f3 la obra al mediod\u00eda.", true);
        work(yeisonA, w3.plusDays(4), START, END, LUNCH, slab.getTask(), null, true);

        LocalDate w4 = w1.plusWeeks(3);
        work(yeisonA, w4, START, END, LUNCH, slab.getTask(), null, true);
        work(yeisonA, w4.plusDays(1), START, END, LUNCH, slab.getTask(), null, true);
        pay(PaymentType.ADVANCE, yeisonA, 50_000, PaymentMethod.CASH, null,
                "Transporte y almuerzo de la semana", true, null, w4.plusDays(1).atTime(18, 30), true);
        work(yeisonA, w4.plusDays(2), START, END, LUNCH, slab.getTask(), null, true);
        pay(PaymentType.SETTLEMENT, yeisonA, null, PaymentMethod.DIGITAL_WALLET, "NQ-1029384756", null, false,
                w4.plusDays(2), w4.plusDays(2).atTime(17, 30), true);
        work(yeisonA, w4.plusDays(3), START, END, LUNCH, slab.getTask(), null, true);
        WorkDay disputed = work(yeisonA, w4.plusDays(4), START, END, LUNCH, "Vaciado de placa", null, false);
        if (disputed != null) {
            disputed.dispute("Ese d\u00eda sal\u00ed a las 5:00 p. m. porque terminamos de vaciar la placa.",
                    w4.plusDays(4).atTime(19, 15));
        }
        pay(PaymentType.ADVANCE, yeisonA, 40_000, PaymentMethod.DIGITAL_WALLET, "NQ-1033571290",
                "Compra de botas de seguridad", true, null, w4.plusDays(4).atTime(18, 0), false);
        LocalDate w5 = w1.plusWeeks(4);
        // Los dias de esta semana quedan por confirmar (si hoy es lunes, el de hoy).
        LocalDate lastPending = today.getDayOfWeek() == DayOfWeek.MONDAY ? today : today.minusDays(1);
        workRange(yeisonA, w5, lastPending, false);

        // Maria Fernanda: empezo en W3; falta registrar su ultimo dia trabajado.
        Assignment mariaA = invite(slab, maria, 75_000, w3);
        workRange(mariaA, w3, w4.plusDays(4), true);
        pay(PaymentType.SETTLEMENT, mariaA, null, PaymentMethod.CASH, null, null, false,
                w4.plusDays(4), w4.plusDays(5).atTime(10, 0), true);
        workRange(mariaA, w5, today.minusDays(2), false);

        // Wilmer: maestro de obra con saldo pendiente por pagar.
        Assignment wilmerA = invite(masonry, wilmer, 120_000, w1.plusWeeks(1));
        workRange(wilmerA, w1.plusWeeks(1), w3.plusDays(4), true);
        pay(PaymentType.SETTLEMENT, wilmerA, null, PaymentMethod.BANK_TRANSFER, "BCO-81002377", null, false,
                w3.plusDays(4), w3.plusDays(5).atTime(9, 0), true);
        workRange(wilmerA, w4, w4.plusDays(4), true);
        pay(PaymentType.ADVANCE, wilmerA, 100_000, PaymentMethod.CASH, null, "Pago del arriendo", true, null,
                w4.plusDays(2).atTime(17, 0), true);
        workRange(wilmerA, w5, today.minusDays(1), false);

        // Deiver: invitacion sin responder desde hace tres dias.
        LocalDateTime invited = LocalDateTime.now(clock).minusDays(3).withHour(18).withMinute(0);
        Assignment deiverA = Assignment.invite(slab, deiver, BigDecimal.valueOf(75_000),
                maxDate(invited.toLocalDate().plusDays(1), today), invited);
        assignmentRepository.save(deiverA);

        // ------------------------------------------------------------ obra planeada
        LocalDate plannedStart = w5.plusWeeks(2);
        ConstructionSite shop = new ConstructionSite(carlos, new ConstructionSite.SiteData(
                "Local comercial Avenida 0",
                "Adecuaci\u00f3n de local: demolici\u00f3n de muros internos, piso nuevo y fachada.",
                "Av. 0 # 11-25", "Centro", CITY, "54001-2-26-0348",
                plannedStart, plannedStart.plusWeeks(10).minusDays(3)), today.minusDays(4).atTime(16, 0));
        shop.openFront(front("Demolici\u00f3n y retiro de escombros", Trade.HELPER, 2, 72_000,
                plannedStart, plannedStart.plusWeeks(2).minusDays(3)));
        siteRepository.save(shop);

        // ------------------------------------------------------------ otro maestro (aislamiento de datos)
        ConstructionSite fence = new ConstructionSite(luz, new ConstructionSite.SiteData(
                "Cerramiento de lote", "Muro de cerramiento en bloque con viga de amarre.",
                "Calle 3 # 25-10", "Antonia Santos", CITY, null, w4, w4.plusWeeks(6).minusDays(3)),
                w4.minusDays(5).atTime(8, 0));
        fence.openFront(front("Excavaci\u00f3n y cimientos", Trade.HELPER, 2, 72_000, w4, w4.plusWeeks(3).minusDays(3)));
        siteRepository.save(fence);
        fence.start();

        log.info("Datos de demostracion cargados: {} usuarios", userRepository.count());
    }

    // ================================================================== ayudas

    private <T extends User> T save(T user) {
        return userRepository.save(user);
    }

    private static User.PersonalData person(DocumentType type, String number, String firstName, String lastName,
                                            LocalDate birthDate, Gender gender, String phone, String email) {
        return new User.PersonalData(type, number, firstName, lastName, birthDate, gender, phone, email, CITY);
    }

    private Worker worker(String document, String firstName, String lastName, LocalDate birthDate, Gender gender,
                          String phone, String email, Trade trade, int years, String neighborhood, boolean tools,
                          LocalDate arl, String emergencyPhone, LocalDate createdOn) {
        return new Worker(person(DocumentType.CC, document, firstName, lastName, birthDate, gender, phone, email),
                new Worker.WorkProfile(trade, years, neighborhood, tools, arl, emergencyPhone),
                passwordEncoder.encode(DEMO_PASSWORD), createdOn.atTime(20, 15));
    }

    private static WorkFront.FrontData front(String task, Trade trade, int openings, long wage, LocalDate start,
                                             LocalDate end) {
        return new WorkFront.FrontData(task, trade, openings, BigDecimal.valueOf(wage), start, end,
                EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), START, END, LUNCH);
    }

    /** Invitacion enviada el viernes anterior al inicio y aceptada esa misma noche. */
    private Assignment invite(WorkFront front, Worker worker, long wage, LocalDate start) {
        LocalDateTime invitedAt = start.minusDays(3).atTime(17, 0);
        Assignment assignment = Assignment.invite(front, worker, BigDecimal.valueOf(wage), start, invitedAt);
        assignment.accept(invitedAt.plusHours(3));
        return assignmentRepository.save(assignment);
    }

    /** Registra todos los dias laborales del rango con el horario del frente, excepto los indicados. */
    private void workRange(Assignment assignment, LocalDate from, LocalDate to, boolean confirmed,
                           LocalDate... skip) {
        List<LocalDate> skipped = List.of(skip);
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            if (assignment.getFront().isWorkingDay(date) && !skipped.contains(date)
                    && assignment.findWorkDayOn(date).isEmpty()) {
                work(assignment, date, START, END, LUNCH, assignment.getFront().getTask(), null, confirmed);
            }
        }
    }

    private void overtimeDay(Assignment assignment, LocalDate date, LocalTime checkOut, String task) {
        work(assignment, date, START, checkOut, LUNCH, task, null, true);
    }

    /**
     * Registra una jornada simulando que se anoto ese mismo dia. Las fechas son
     * relativas a hoy, asi que alguna puede caer en festivo: esa se omite, igual
     * que en una obra real.
     *
     * @return la jornada, o null si el dia era festivo
     */
    private WorkDay work(Assignment assignment, LocalDate date, LocalTime in, LocalTime out, int breakMinutes,
                         String task, String notes, boolean confirmed) {
        if (ColombianHolidays.isHoliday(date)) {
            return null;
        }
        WorkDay day = assignment.registerWorkDay(date, new WorkDay.TimeRecord(in, out, breakMinutes, task, notes),
                date, date.atTime(out).plusMinutes(20));
        if (confirmed) {
            day.confirm(date.atTime(out).plusHours(2));
        }
        return day;
    }

    /** Usa las mismas estrategias de pago que la aplicacion; el monto de las liquidaciones lo calculan ellas. */
    private void pay(PaymentType type, Assignment assignment, Integer amount, PaymentMethod method, String reference,
                     String reason, boolean requestedByWorker, LocalDate periodEnd, LocalDateTime paidAt,
                     boolean confirmed) {
        PaymentCommand command = new PaymentCommand(type, amount == null ? null : BigDecimal.valueOf(amount), method,
                reference, reason, requestedByWorker, periodEnd);
        // Si un festivo dejo la semana con menos jornadas, el anticipo no puede superar el saldo: se omite.
        if (type == PaymentType.ADVANCE && assignment.calculateBalance().compareTo(command.amount()) < 0) {
            return;
        }
        String receipt = "JOR-%06d".formatted(paymentRepository.nextReceiptSequence());
        Payment payment = strategies.get(type).create(assignment, command, receipt, paidAt);
        if (confirmed) {
            payment.confirmReceipt(paidAt.plusMinutes(30));
        }
        paymentRepository.save(payment);
    }

    private static LocalDate maxDate(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }
}

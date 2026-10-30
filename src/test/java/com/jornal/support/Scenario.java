package com.jornal.support;

import com.jornal.entity.Assignment;
import com.jornal.entity.ConstructionSite;
import com.jornal.entity.Foreman;
import com.jornal.entity.Trade;
import com.jornal.entity.User;
import com.jornal.entity.WorkFront;
import com.jornal.entity.Worker;
import com.jornal.repository.AssignmentRepository;
import com.jornal.repository.ConstructionSiteRepository;
import com.jornal.repository.UserRepository;
import com.jornal.security.JornalUserDetails;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Arma datos persistidos para las pruebas de integracion: maestros,
 * trabajadores, obras y asignaciones, con clave real cifrada para poder
 * iniciar sesion por el formulario o por la API.
 */
public class Scenario {

    public static final String PASSWORD = "Prueba2026clave";

    private final UserRepository users;
    private final ConstructionSiteRepository sites;
    private final AssignmentRepository assignments;
    private final PasswordEncoder encoder;
    private final Clock clock;

    public Scenario(UserRepository users, ConstructionSiteRepository sites, AssignmentRepository assignments,
                    PasswordEncoder encoder, Clock clock) {
        this.users = users;
        this.sites = sites;
        this.assignments = assignments;
        this.encoder = encoder;
        this.clock = clock;
    }

    public Foreman foreman() {
        return users.save(new Foreman(TestData.person("Carlos", "Rincon", LocalDate.of(1980, 1, 1)), null,
                encoder.encode(PASSWORD), LocalDateTime.now(clock)));
    }

    public Worker worker() {
        return users.save(new Worker(TestData.person("Yeison", "Quintero", LocalDate.of(1998, 6, 21)),
                new Worker.WorkProfile(Trade.HELPER, 3, "La Libertad", false, LocalDate.of(2027, 12, 31), "3001112233"),
                encoder.encode(PASSWORD), LocalDateTime.now(clock)));
    }

    /** Obra en curso (5 oct - 31 dic) con un frente de ayudantes (5 oct - 18 dic, $80.000). */
    public WorkFront frontOf(Foreman foreman) {
        ConstructionSite site = TestData.siteInProgress(foreman);
        WorkFront front = TestData.front(site);
        sites.save(site);
        return front;
    }

    /** Asignacion aceptada desde el 5 de octubre. */
    public Assignment assignment(WorkFront front, Worker worker) {
        Assignment assignment = Assignment.invite(front, worker, TestData.WAGE, LocalDate.of(2026, 10, 5),
                LocalDateTime.of(2026, 10, 2, 17, 0));
        assignment.accept(LocalDateTime.of(2026, 10, 2, 19, 0));
        return assignments.save(assignment);
    }

    public JornalUserDetails login(User user) {
        return JornalUserDetails.from(user, LocalDateTime.now(clock));
    }

    public static UUID idOf(User user) {
        return user.getId();
    }
}

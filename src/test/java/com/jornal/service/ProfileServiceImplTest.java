package com.jornal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.jornal.dto.form.ForemanProfileForm;
import com.jornal.dto.form.WorkerProfileForm;
import com.jornal.dto.view.ProfileSummary;
import com.jornal.entity.AssignmentStatus;
import com.jornal.entity.Foreman;
import com.jornal.entity.Role;
import com.jornal.entity.Worker;
import com.jornal.exception.ResourceNotFoundException;
import com.jornal.repository.AssignmentRepository;
import com.jornal.repository.ForemanRepository;
import com.jornal.repository.UserRepository;
import com.jornal.repository.WorkerRepository;
import com.jornal.service.impl.ProfileServiceImpl;
import com.jornal.support.Clocks;
import com.jornal.support.TestData;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Perfil de la cuenta con @InjectMocks: el servicio solo depende de repositorios
 * y del reloj, asi que Mockito lo arma por constructor con los simulados. El reloj
 * se simula devolviendo siempre el mismo instante (jueves 29 de octubre de 2026).
 *
 * <p>Los demas servicios se arman a mano en cada prueba porque reciben
 * colaboradores reales (ViewMapper, WagePolicy, las estrategias de pago) que no
 * tiene sentido simular, y @InjectMocks solo sabe inyectar simulados.
 */
@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ForemanRepository foremanRepository;
    @Mock
    private WorkerRepository workerRepository;
    @Mock
    private AssignmentRepository assignmentRepository;
    @Mock
    private Clock clock;

    @InjectMocks
    private ProfileServiceImpl service;

    @BeforeEach
    void fixedClock() {
        Clock now = Clocks.fixedAtTestNow();
        lenient().when(clock.instant()).thenReturn(now.instant());
        lenient().when(clock.getZone()).thenReturn(now.getZone());
    }

    @Test
    void summary_trabajador_usaSusCalificacionesYSusJornadasConfirmadas() {
        Worker worker = TestData.worker();
        UUID id = worker.getId();
        when(userRepository.findById(id)).thenReturn(Optional.of(worker));
        when(workerRepository.averageRating(id)).thenReturn(4.5);
        when(workerRepository.countRatings(id)).thenReturn(2L);
        when(workerRepository.countConfirmedWorkDays(id)).thenReturn(17L);
        when(assignmentRepository.countByWorkerIdAndStatus(id, AssignmentStatus.COMPLETED)).thenReturn(3L);

        ProfileSummary summary = service.summary(id);

        assertThat(summary.role()).isEqualTo(Role.WORKER);
        assertThat(summary.rating()).isEqualTo(4.5);
        assertThat(summary.ratingCount()).isEqualTo(2);
        assertThat(summary.confirmedWorkDays()).isEqualTo(17);
        assertThat(summary.completedAssignments()).isEqualTo(3);
        // Nacio el 21 de junio de 1998; el reloj dice 29 de octubre de 2026.
        assertThat(summary.age()).isEqualTo(28);
        verifyNoInteractions(foremanRepository);
    }

    @Test
    void summary_maestro_usaLasCalificacionesDelMaestroYNoCuentaJornadas() {
        Foreman foreman = TestData.foreman();
        UUID id = foreman.getId();
        when(userRepository.findById(id)).thenReturn(Optional.of(foreman));
        when(foremanRepository.averageRating(id)).thenReturn(null);
        when(foremanRepository.countRatings(id)).thenReturn(0L);
        when(assignmentRepository.countByFrontSiteForemanIdAndStatus(id, AssignmentStatus.COMPLETED)).thenReturn(5L);

        ProfileSummary summary = service.summary(id);

        assertThat(summary.role()).isEqualTo(Role.FOREMAN);
        assertThat(summary.rating()).isNull();
        assertThat(summary.confirmedWorkDays()).isZero();
        assertThat(summary.completedAssignments()).isEqualTo(5);
        verifyNoInteractions(workerRepository);
    }

    @Test
    void summary_usuarioInexistente_lanzaNoEncontrado() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.summary(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("code").isEqualTo("usuario.no.encontrado");
    }

    @Test
    void updateForemanProfile_cambiaContactoYEmpresaDelMaestro() {
        Foreman foreman = TestData.foreman();
        when(foremanRepository.findById(foreman.getId())).thenReturn(Optional.of(foreman));
        ForemanProfileForm form = ForemanProfileForm.from(foreman);
        form.setPhone("3115550000");
        form.setCompanyName("Construcciones del Norte");

        service.updateForemanProfile(foreman.getId(), form);

        assertThat(foreman.getPhone()).isEqualTo("3115550000");
        assertThat(foreman.getCompanyName()).isEqualTo("Construcciones del Norte");
    }

    @Test
    void updateWorkerProfile_trabajadorInexistente_lanzaNoEncontrado() {
        UUID id = UUID.randomUUID();
        when(workerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateWorkerProfile(id, new WorkerProfileForm()))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("code").isEqualTo("trabajador.no.encontrado");
    }
}

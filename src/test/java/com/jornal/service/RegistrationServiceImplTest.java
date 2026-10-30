package com.jornal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jornal.dto.form.ForemanRegistrationForm;
import com.jornal.dto.form.PersonalDataForm;
import com.jornal.dto.form.WorkerRegistrationForm;
import com.jornal.entity.DocumentType;
import com.jornal.entity.Gender;
import com.jornal.entity.Trade;
import com.jornal.entity.User;
import com.jornal.entity.Worker;
import com.jornal.repository.UserRepository;
import com.jornal.service.impl.RegistrationServiceImpl;
import com.jornal.support.Clocks;
import com.jornal.support.TestData;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private RegistrationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RegistrationServiceImpl(userRepository, passwordEncoder, Clocks.fixedAtTestNow());
    }

    @Test
    void registerWorker_guardaLaClaveCifradaYNuncaEnTextoPlano() {
        when(passwordEncoder.encode("Clave2026segura")).thenReturn("$2a$12$cifrada");
        WorkerRegistrationForm form = workerForm(LocalDate.of(1998, 6, 21));

        service.registerWorker(form);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue()).isInstanceOf(Worker.class);
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("$2a$12$cifrada").isNotEqualTo("Clave2026segura");
        assertThat(saved.getValue().getEmail()).isEqualTo("yeison@prueba.co");
    }

    @Test
    void registerWorker_menorDeEdad_esRechazado() {
        WorkerRegistrationForm form = workerForm(TestData.TODAY.minusYears(17));

        assertThatThrownBy(() -> service.registerWorker(form)).extracting("code").isEqualTo("registro.menor.de.edad");
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerWorker_arlVencida_esRechazado() {
        WorkerRegistrationForm form = workerForm(LocalDate.of(1998, 6, 21));
        form.setArlValidUntil(TestData.TODAY.minusDays(1));

        assertThatThrownBy(() -> service.registerWorker(form)).extracting("code").isEqualTo("registro.arl.vencida");
    }

    @Test
    void registerForeman_correoYaRegistrado_daUnMensajeGenericoQueNoConfirmaLaCuenta() {
        ForemanRegistrationForm form = fill(new ForemanRegistrationForm(), LocalDate.of(1980, 1, 1));
        when(userRepository.existsByEmail("yeison@prueba.co")).thenReturn(true);

        assertThatThrownBy(() -> service.registerForeman(form)).extracting("code").isEqualTo("registro.datos.en.uso");
        verify(passwordEncoder, never()).encode(any());
    }

    private static WorkerRegistrationForm workerForm(LocalDate birthDate) {
        WorkerRegistrationForm form = fill(new WorkerRegistrationForm(), birthDate);
        form.setTrade(Trade.HELPER);
        form.setYearsOfExperience(2);
        form.setNeighborhood("La Libertad");
        form.setArlValidUntil(TestData.TODAY.plusMonths(6));
        form.setEmergencyPhone("3001112233");
        return form;
    }

    private static <T extends PersonalDataForm> T fill(T form, LocalDate birthDate) {
        form.setDocumentType(DocumentType.CC);
        form.setDocumentNumber("1090456789");
        form.setFirstName("Yeison");
        form.setLastName("Quintero");
        form.setBirthDate(birthDate);
        form.setGender(Gender.MALE);
        form.setPhone("3112345678");
        form.setEmail(" Yeison@Prueba.co ");
        form.setCity("C\u00facuta");
        form.setPassword("Clave2026segura");
        form.setPasswordConfirmation("Clave2026segura");
        form.setAcceptsDataPolicy(true);
        return form;
    }
}

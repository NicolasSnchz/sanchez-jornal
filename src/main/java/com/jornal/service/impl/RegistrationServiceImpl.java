package com.jornal.service.impl;

import com.jornal.dto.form.ForemanRegistrationForm;
import com.jornal.dto.form.PersonalDataForm;
import com.jornal.dto.form.WorkerRegistrationForm;
import com.jornal.entity.Foreman;
import com.jornal.entity.User;
import com.jornal.entity.Worker;
import com.jornal.exception.BusinessRuleException;
import com.jornal.repository.UserRepository;
import com.jornal.service.RegistrationService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RegistrationServiceImpl implements RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationServiceImpl.class);
    private static final int MAX_AGE = 90;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public RegistrationServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    public UUID registerForeman(ForemanRegistrationForm form) {
        validatePersonalData(form);
        Foreman foreman = new Foreman(form.toPersonalData(), form.getCompanyName(),
                passwordEncoder.encode(form.getPassword()), LocalDateTime.now(clock));
        userRepository.save(foreman);
        log.info("Cuenta de maestro creada: {}", foreman.getId());
        return foreman.getId();
    }

    @Override
    public UUID registerWorker(WorkerRegistrationForm form) {
        validatePersonalData(form);
        if (form.getArlValidUntil().isBefore(LocalDate.now(clock))) {
            throw new BusinessRuleException("registro.arl.vencida");
        }
        Worker worker = new Worker(form.toPersonalData(), form.toWorkProfile(),
                passwordEncoder.encode(form.getPassword()), LocalDateTime.now(clock));
        userRepository.save(worker);
        log.info("Cuenta de trabajador creada: {}", worker.getId());
        return worker.getId();
    }

    /**
     * Correo y documento repetidos dan el mismo mensaje generico: el formulario
     * de registro no debe servir para averiguar quien ya tiene cuenta.
     */
    private void validatePersonalData(PersonalDataForm form) {
        int age = Period.between(form.getBirthDate(), LocalDate.now(clock)).getYears();
        if (age < User.ADULT_AGE) {
            throw new BusinessRuleException("registro.menor.de.edad", User.ADULT_AGE);
        }
        if (age > MAX_AGE) {
            throw new BusinessRuleException("registro.fecha.nacimiento.invalida");
        }
        boolean emailTaken = userRepository.existsByEmail(User.normalizeEmail(form.getEmail()));
        boolean documentTaken = userRepository.existsByDocumentTypeAndDocumentNumber(
                form.getDocumentType(), form.getDocumentNumber().trim());
        if (emailTaken || documentTaken) {
            throw new BusinessRuleException("registro.datos.en.uso");
        }
    }
}

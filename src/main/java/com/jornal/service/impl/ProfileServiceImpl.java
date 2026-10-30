package com.jornal.service.impl;

import com.jornal.dto.form.ForemanProfileForm;
import com.jornal.dto.form.WorkerProfileForm;
import com.jornal.dto.view.ProfileSummary;
import com.jornal.entity.AssignmentStatus;
import com.jornal.entity.Foreman;
import com.jornal.entity.User;
import com.jornal.entity.Worker;
import com.jornal.exception.ResourceNotFoundException;
import com.jornal.repository.AssignmentRepository;
import com.jornal.repository.ForemanRepository;
import com.jornal.repository.UserRepository;
import com.jornal.repository.WorkerRepository;
import com.jornal.service.ProfileService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProfileServiceImpl implements ProfileService {

    private static final Logger log = LoggerFactory.getLogger(ProfileServiceImpl.class);

    private final UserRepository userRepository;
    private final ForemanRepository foremanRepository;
    private final WorkerRepository workerRepository;
    private final AssignmentRepository assignmentRepository;
    private final Clock clock;

    public ProfileServiceImpl(UserRepository userRepository, ForemanRepository foremanRepository,
                              WorkerRepository workerRepository, AssignmentRepository assignmentRepository,
                              Clock clock) {
        this.userRepository = userRepository;
        this.foremanRepository = foremanRepository;
        this.workerRepository = workerRepository;
        this.assignmentRepository = assignmentRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileSummary summary(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("usuario.no.encontrado"));
        boolean isWorker = user instanceof Worker;
        Double rating = isWorker ? workerRepository.averageRating(userId) : foremanRepository.averageRating(userId);
        long ratingCount = isWorker ? workerRepository.countRatings(userId) : foremanRepository.countRatings(userId);
        long completed = isWorker
                ? assignmentRepository.countByWorkerIdAndStatus(userId, AssignmentStatus.COMPLETED)
                : assignmentRepository.countByFrontSiteForemanIdAndStatus(userId, AssignmentStatus.COMPLETED);
        return new ProfileSummary(user.getRole(), user.getFullName(), user.getInitials(), user.getEmail(),
                user.getDocumentType(), user.getDocumentNumber(), user.calculateAge(LocalDate.now(clock)),
                user.getCreatedAt(), rating, ratingCount,
                isWorker ? workerRepository.countConfirmedWorkDays(userId) : 0, completed);
    }

    @Override
    @Transactional(readOnly = true)
    public ForemanProfileForm getForemanProfile(UUID foremanId) {
        return ForemanProfileForm.from(findForeman(foremanId));
    }

    @Override
    public void updateForemanProfile(UUID foremanId, ForemanProfileForm form) {
        Foreman foreman = findForeman(foremanId);
        foreman.updateContact(form.getPhone(), form.getCity());
        foreman.updateCompanyName(form.getCompanyName());
        log.info("Perfil del maestro {} actualizado", foremanId);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkerProfileForm getWorkerProfile(UUID workerId) {
        return WorkerProfileForm.from(findWorker(workerId));
    }

    @Override
    public void updateWorkerProfile(UUID workerId, WorkerProfileForm form) {
        Worker worker = findWorker(workerId);
        worker.updateContact(form.getPhone(), form.getCity());
        worker.updateWorkProfile(form.toWorkProfile());
        worker.changeAvailability(form.isAcceptingOffers());
        log.info("Perfil del trabajador {} actualizado", workerId);
    }

    private Foreman findForeman(UUID id) {
        return foremanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("maestro.no.encontrado"));
    }

    private Worker findWorker(UUID id) {
        return workerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("trabajador.no.encontrado"));
    }
}

package com.jornal.service;

import com.jornal.dto.form.ForemanProfileForm;
import com.jornal.dto.form.WorkerProfileForm;
import com.jornal.dto.view.ProfileSummary;
import com.jornal.security.Policies;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;

/** Perfil propio. Nadie puede ver ni editar el perfil completo de otra persona. */
public interface ProfileService {

    @PreAuthorize(Policies.SELF)
    ProfileSummary summary(UUID userId);

    @PreAuthorize(Policies.FOREMAN_SELF)
    ForemanProfileForm getForemanProfile(UUID foremanId);

    @PreAuthorize(Policies.FOREMAN_SELF)
    void updateForemanProfile(UUID foremanId, ForemanProfileForm form);

    @PreAuthorize(Policies.WORKER_SELF)
    WorkerProfileForm getWorkerProfile(UUID workerId);

    @PreAuthorize(Policies.WORKER_SELF)
    void updateWorkerProfile(UUID workerId, WorkerProfileForm form);
}

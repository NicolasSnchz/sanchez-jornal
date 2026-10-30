package com.jornal.service;

import com.jornal.dto.view.ForemanDashboard;
import com.jornal.dto.view.WorkerDashboard;
import com.jornal.security.Policies;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;

/** Paneles de inicio: lo que cada rol necesita ver y resolver hoy. */
public interface DashboardService {

    @PreAuthorize(Policies.FOREMAN_SELF)
    ForemanDashboard foremanDashboard(UUID foremanId);

    @PreAuthorize(Policies.WORKER_SELF)
    WorkerDashboard workerDashboard(UUID workerId);
}

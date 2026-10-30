package com.jornal.repository;

import com.jornal.entity.WorkDay;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkDayRepository extends JpaRepository<WorkDay, UUID> {

    Optional<WorkDay> findByIdAndAssignmentFrontSiteForemanId(UUID id, UUID foremanId);

    Optional<WorkDay> findByIdAndAssignmentWorkerId(UUID id, UUID workerId);
}

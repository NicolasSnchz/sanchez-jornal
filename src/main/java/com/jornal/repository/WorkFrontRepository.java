package com.jornal.repository;

import com.jornal.entity.WorkFront;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkFrontRepository extends JpaRepository<WorkFront, UUID> {

    Optional<WorkFront> findByIdAndSiteForemanId(UUID id, UUID foremanId);
}

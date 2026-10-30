package com.jornal.repository;

import com.jornal.entity.ConstructionSite;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Las consultas incluyen siempre el id del maestro: la verificacion de propiedad
 * se resuelve en la base de datos y no despues de cargar la obra.
 */
public interface ConstructionSiteRepository extends JpaRepository<ConstructionSite, UUID> {

    Optional<ConstructionSite> findByIdAndForemanId(UUID id, UUID foremanId);

    List<ConstructionSite> findByForemanIdOrderByCreatedAtDesc(UUID foremanId);
}

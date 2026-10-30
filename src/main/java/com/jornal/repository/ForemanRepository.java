package com.jornal.repository;

import com.jornal.entity.Foreman;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ForemanRepository extends JpaRepository<Foreman, UUID> {

    /** Promedio de las calificaciones que los trabajadores le han dado al maestro. */
    @Query("""
            select avg(a.foremanRating) from Assignment a
            where a.front.site.foreman.id = :foremanId and a.foremanRating is not null
            """)
    Double averageRating(@Param("foremanId") UUID foremanId);

    @Query("""
            select count(a) from Assignment a
            where a.front.site.foreman.id = :foremanId and a.foremanRating is not null
            """)
    long countRatings(@Param("foremanId") UUID foremanId);
}

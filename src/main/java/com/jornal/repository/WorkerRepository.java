package com.jornal.repository;

import com.jornal.entity.Trade;
import com.jornal.entity.Worker;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkerRepository extends JpaRepository<Worker, UUID> {

    /**
     * Busqueda de candidatos. Todos los valores viajan como parametros enlazados
     * (nunca concatenados al JPQL), por eso un texto como ' OR '1'='1 se trata
     * como un nombre literal y no altera la consulta (OWASP A03).
     */
    @Query("""
            select w from Worker w
            where w.active = true and w.acceptingOffers = true
              and w.trade = :trade
              and lower(w.city) = lower(:city)
              and lower(concat(w.firstName, ' ', w.lastName)) like lower(concat('%', :name, '%'))
            order by w.yearsOfExperience desc, w.lastName asc
            """)
    List<Worker> searchCandidates(@Param("trade") Trade trade, @Param("city") String city, @Param("name") String name,
                                  Pageable pageable);

    /** Promedio de las calificaciones que los maestros le han dado al trabajador. */
    @Query("select avg(a.workerRating) from Assignment a where a.worker.id = :workerId and a.workerRating is not null")
    Double averageRating(@Param("workerId") UUID workerId);

    @Query("select count(a) from Assignment a where a.worker.id = :workerId and a.workerRating is not null")
    long countRatings(@Param("workerId") UUID workerId);

    /** Experiencia comprobada dentro de Jornal: jornadas confirmadas. */
    @Query("select count(d) from WorkDay d where d.assignment.worker.id = :workerId and d.confirmedAt is not null")
    long countConfirmedWorkDays(@Param("workerId") UUID workerId);
}

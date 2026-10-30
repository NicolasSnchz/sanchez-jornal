package com.jornal.repository;

import com.jornal.entity.Assignment;
import com.jornal.entity.AssignmentStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {

    /** Asignacion vista por el maestro: solo si el frente pertenece a una de sus obras. */
    Optional<Assignment> findByIdAndFrontSiteForemanId(UUID id, UUID foremanId);

    /**
     * Igual que la anterior, pero incrementa la version de la asignacion al
     * confirmar la transaccion. Si dos pagos se registran a la vez sobre la misma
     * asignacion, el segundo falla en lugar de pagar dos veces el mismo saldo.
     */
    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("select a from Assignment a where a.id = :id and a.front.site.foreman.id = :foremanId")
    Optional<Assignment> findForPayment(@Param("id") UUID id, @Param("foremanId") UUID foremanId);

    /** Asignacion vista por el trabajador: solo si es suya. */
    Optional<Assignment> findByIdAndWorkerId(UUID id, UUID workerId);

    List<Assignment> findByWorkerIdOrderByInvitedAtDesc(UUID workerId);

    List<Assignment> findByWorkerIdAndStatusOrderByInvitedAtDesc(UUID workerId, AssignmentStatus status);

    List<Assignment> findByFrontSiteForemanIdAndStatusInOrderByInvitedAtDesc(UUID foremanId,
                                                                            Collection<AssignmentStatus> statuses);

    long countByWorkerIdAndStatus(UUID workerId, AssignmentStatus status);

    long countByFrontSiteForemanIdAndStatus(UUID foremanId, AssignmentStatus status);

    /** Evita invitar dos veces al mismo trabajador al mismo frente. */
    boolean existsByWorkerIdAndFrontIdAndStatusIn(UUID workerId, UUID frontId, Collection<AssignmentStatus> statuses);

    /**
     * Cruce de agenda: el trabajador ya esta trabajando (asignacion en curso) en
     * un periodo [inicio, fin del frente] que se cruza con el pedido. Las
     * invitaciones abiertas no bloquean: el trabajador decide cual acepta, y al
     * aceptar se vuelve a revisar este cruce.
     */
    @Query("""
            select case when count(a) > 0 then true else false end from Assignment a
            where a.worker.id = :workerId
              and a.status = com.jornal.entity.AssignmentStatus.IN_PROGRESS
              and a.startDate <= :to and a.front.endDate >= :from
            """)
    boolean existsInProgressOverlapping(@Param("workerId") UUID workerId,
                                        @Param("from") LocalDate from,
                                        @Param("to") LocalDate to);
}

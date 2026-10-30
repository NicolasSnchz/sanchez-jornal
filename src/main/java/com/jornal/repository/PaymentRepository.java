package com.jornal.repository;

import com.jornal.entity.Payment;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByIdAndAssignmentFrontSiteForemanId(UUID id, UUID foremanId);

    Optional<Payment> findByIdAndAssignmentWorkerId(UUID id, UUID workerId);

    @Query("""
            select p from Payment p
            where p.assignment.front.site.foreman.id = :foremanId
              and p.paidAt >= :from and p.paidAt < :to
            order by p.paidAt desc
            """)
    List<Payment> findByForemanBetween(@Param("foremanId") UUID foremanId,
                                       @Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to);

    /** Siguiente consecutivo de comprobante, tomado de una secuencia de la base de datos. */
    @Query(value = "select nextval('receipt_number_seq')", nativeQuery = true)
    long nextReceiptSequence();
}

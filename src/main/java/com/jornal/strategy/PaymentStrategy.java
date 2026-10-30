package com.jornal.strategy;

import com.jornal.entity.Assignment;
import com.jornal.entity.Payment;
import com.jornal.entity.PaymentType;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Patron Strategy: cada tipo de pago tiene sus propias reglas para validar y
 * calcular el monto. Agregar un tipo nuevo es agregar una clase nueva, sin tocar
 * el servicio (principio abierto/cerrado).
 */
public interface PaymentStrategy {

    PaymentType type();

    /** Valida las reglas del tipo de pago y calcula el monto, sin registrar nada. */
    PaymentQuote quote(Assignment assignment, PaymentCommand command, LocalDate today);

    /** Crea el pago ya validado. */
    Payment create(Assignment assignment, PaymentCommand command, String receiptNumber, LocalDateTime now);
}

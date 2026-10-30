package com.jornal.entity;

/** Estado derivado de un pago: se calcula, no se guarda. */
public enum PaymentStatus {
    PENDING, CONFIRMED, CANCELLED
}

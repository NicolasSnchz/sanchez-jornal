package com.jornal.dto.view;

import java.time.LocalDateTime;

/**
 * Algo que requiere atencion en el panel. El texto se arma con un codigo de
 * mensaje y sus argumentos, igual que los errores.
 */
public record PendingItem(
        String kind,
        String messageCode,
        Object[] args,
        String detail,
        String link,
        String linkLabelCode,
        LocalDateTime since) {
}

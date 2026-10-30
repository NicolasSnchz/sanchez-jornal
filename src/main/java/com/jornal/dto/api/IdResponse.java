package com.jornal.dto.api;

import java.util.UUID;

/** Respuesta de una creacion: el id del recurso (la URL completa va en el header Location). */
public record IdResponse(UUID id) {
}

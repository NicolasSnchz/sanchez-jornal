package com.jornal.exception;

/**
 * El recurso no existe o no pertenece al usuario autenticado.
 * Ambos casos responden 404 a proposito: un 403 confirmaria que el id existe
 * y le daria pistas a quien intenta enumerar recursos ajenos (IDOR).
 */
public class ResourceNotFoundException extends RuntimeException {

    private final String code;

    public ResourceNotFoundException(String code) {
        super(code);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

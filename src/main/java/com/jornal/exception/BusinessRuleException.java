package com.jornal.exception;

import java.util.Arrays;

/**
 * Regla de negocio incumplida (por ejemplo, registrar una jornada en un periodo
 * ya liquidado). Lleva un codigo de mensaje, no un texto: el texto en espanol vive
 * en messages.properties y las pruebas comparan codigos estables.
 */
public class BusinessRuleException extends RuntimeException {

    private final String code;
    private final transient Object[] args;

    public BusinessRuleException(String code, Object... args) {
        super(code);
        this.code = code;
        this.args = args == null ? new Object[0] : args.clone();
    }

    public String getCode() {
        return code;
    }

    public Object[] getArgs() {
        return args.clone();
    }

    @Override
    public String toString() {
        return "BusinessRuleException[" + code + ", args=" + Arrays.toString(args) + "]";
    }
}

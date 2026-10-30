package com.jornal.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LoginAttemptListenerTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
        "maestro@jornal.co, m***@jornal.co",
        "a@b.co, a***@b.co",
        "sin-arroba, ***",
        "@jornal.co, ***"
    })
    void mask_ocultaElCorreoEnLosLogs(String email, String expected) {
        assertThat(LoginAttemptListener.mask(email)).isEqualTo(expected);
    }
}

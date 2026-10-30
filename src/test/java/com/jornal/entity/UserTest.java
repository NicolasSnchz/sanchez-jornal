package com.jornal.entity;

import static com.jornal.support.TestData.TODAY;
import static org.assertj.core.api.Assertions.assertThat;

import com.jornal.support.TestData;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class UserTest {

    private static final Duration LOCK = Duration.ofMinutes(15);

    @Test
    void registerFailedLogin_alQuintoIntento_bloqueaQuinceMinutos() {
        Worker user = TestData.worker();
        LocalDateTime now = TestData.NOW;

        for (int i = 0; i < 4; i++) {
            assertThat(user.registerFailedLogin(now, 5, LOCK)).isFalse();
        }

        assertThat(user.registerFailedLogin(now, 5, LOCK)).isTrue();
        assertThat(user.isLocked(now.plusMinutes(14))).isTrue();
        assertThat(user.isLocked(now.plusMinutes(15))).isFalse();
        assertThat(user.getFailedLoginAttempts()).isZero();
    }

    @Test
    void registerSuccessfulLogin_reiniciaElContador() {
        Worker user = TestData.worker();
        user.registerFailedLogin(TestData.NOW, 5, LOCK);
        user.registerFailedLogin(TestData.NOW, 5, LOCK);

        user.registerSuccessfulLogin();

        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getLockedUntil()).isNull();
    }

    @ParameterizedTest(name = "nacido el {0}: adulto = {1}")
    @CsvSource({
        "2008-10-29, true",    // cumple 18 hoy
        "2008-10-30, false",   // cumple 18 manana
        "1990-01-01, true"
    })
    void isAdult_calculaLaEdadCumplida(LocalDate birthDate, boolean adult) {
        assertThat(TestData.workerBornOn(birthDate).isAdult(TODAY)).isEqualTo(adult);
    }

    @Test
    void normalizeEmail_quitaEspaciosYMayusculas() {
        assertThat(User.normalizeEmail("  Maestro@Jornal.CO ")).isEqualTo("maestro@jornal.co");
    }

    @Test
    void getInitials_usaNombreYApellido() {
        assertThat(TestData.foreman().getInitials()).isEqualTo("CR");
    }
}

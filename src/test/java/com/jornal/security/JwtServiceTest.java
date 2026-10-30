package com.jornal.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jornal.entity.Foreman;
import com.jornal.support.Clocks;
import com.jornal.support.MutableClock;
import com.jornal.support.TestData;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "secreto-de-prueba-con-mas-de-treinta-y-dos-bytes-2026";

    private MutableClock clock;
    private JwtService jwtService;
    private JornalUserDetails user;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(TestData.NOW, Clocks.BOGOTA);
        jwtService = new JwtService(new SecurityProperties(SECRET, Duration.ofHours(1), 5, Duration.ofMinutes(15), 4),
                clock);
        Foreman foreman = TestData.foreman();
        user = JornalUserDetails.from(foreman, TestData.NOW);
    }

    @Test
    void generateToken_soloLlevaElIdYElRol() {
        String token = jwtService.generateToken(user);
        String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);

        assertThat(jwtService.validateAndGetUserId(token)).isEqualTo(user.id());
        assertThat(payload).contains("\"role\":\"FOREMAN\"").doesNotContain(user.getUsername()).doesNotContain("$");
    }

    @Test
    void validate_tokenExpirado_esRechazado() {
        String token = jwtService.generateToken(user);
        clock.advance(Duration.ofMinutes(61));

        assertThatThrownBy(() -> jwtService.validateAndGetUserId(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void validate_firmadoConOtraClave_esRechazado() {
        String forged = Jwts.builder().issuer("jornal").subject(user.id().toString())
                .expiration(Date.from(clock.instant().plusSeconds(600)))
                .signWith(Keys.hmacShaKeyFor("otra-clave-distinta-de-mas-de-treinta-y-dos-bytes".getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThatThrownBy(() -> jwtService.validateAndGetUserId(forged)).isInstanceOf(JwtException.class);
    }

    @Test
    void validate_sinFirma_algNone_esRechazado() {
        String unsigned = Jwts.builder().issuer("jornal").subject(user.id().toString())
                .expiration(Date.from(clock.instant().plusSeconds(600))).compact();

        assertThatThrownBy(() -> jwtService.validateAndGetUserId(unsigned)).isInstanceOf(JwtException.class);
    }

    @Test
    void validate_otroEmisor_esRechazado() {
        String foreign = Jwts.builder().issuer("otra-app").subject(user.id().toString())
                .expiration(Date.from(clock.instant().plusSeconds(600)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();

        assertThatThrownBy(() -> jwtService.validateAndGetUserId(foreign)).isInstanceOf(JwtException.class);
    }

    @Test
    void constructor_secretoCorto_fallaAlArrancar() {
        SecurityProperties weak = new SecurityProperties("corto", Duration.ofHours(1), 5, Duration.ofMinutes(15), 4);

        assertThatThrownBy(() -> new JwtService(weak, clock)).isInstanceOf(RuntimeException.class);
    }
}

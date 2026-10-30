package com.jornal.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Emite y valida los JWT de la API.
 *
 * <ul>
 *   <li>Algoritmo fijo HS256: el servidor no acepta el algoritmo que declare el token
 *       (evita el ataque de confusion de algoritmo y el alg "none").</li>
 *   <li>Vida corta (15 minutos por defecto): limita el dano si el token se filtra.</li>
 *   <li>El payload solo lleva el id y el rol; nunca correo, documento ni nombre,
 *       porque el payload de un JWT es legible por cualquiera (Base64, no cifrado).</li>
 * </ul>
 */
@Service
public class JwtService {

    static final String ISSUER = "jornal";
    static final String ALGORITHM = "HS256";
    private static final String ROLE_CLAIM = "role";

    private final SecretKey key;
    private final Duration expiration;
    private final Clock clock;

    public JwtService(SecurityProperties properties, Clock clock) {
        // hmacShaKeyFor rechaza claves de menos de 256 bits: falla al arrancar, no en produccion
        this.key = Keys.hmacShaKeyFor(properties.jwtSecret().getBytes(StandardCharsets.UTF_8));
        this.expiration = properties.jwtExpiration();
        this.clock = clock;
    }

    public String generateToken(JornalUserDetails user) {
        Instant now = clock.instant();
        return Jwts.builder()
                .issuer(ISSUER)
                .subject(user.id().toString())
                .claim(ROLE_CLAIM, user.role().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Valida firma, emisor, expiracion y algoritmo.
     *
     * @return el id del usuario (claim sub)
     * @throws JwtException si el token no es valido por cualquier motivo
     */
    public UUID validateAndGetUserId(String token) {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(ISSUER)
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token);
        if (!ALGORITHM.equals(jws.getHeader().getAlgorithm())) {
            throw new JwtException("Algoritmo de firma no permitido");
        }
        try {
            return UUID.fromString(jws.getPayload().getSubject());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new JwtException("Sujeto del token invalido", e);
        }
    }

    public long expirationSeconds() {
        return expiration.toSeconds();
    }
}

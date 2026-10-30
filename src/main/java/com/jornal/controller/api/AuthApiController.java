package com.jornal.controller.api;

import com.jornal.dto.api.ApiError;
import com.jornal.dto.api.LoginRequest;
import com.jornal.dto.api.TokenResponse;
import com.jornal.dto.view.ProfileSummary;
import com.jornal.security.JornalUserDetails;
import com.jornal.security.JwtService;
import com.jornal.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticacion", description = "Obtener el token JWT y consultar la cuenta autenticada")
public class AuthApiController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final ProfileService profileService;
    private final MessageSource messageSource;
    private final Clock clock;

    public AuthApiController(AuthenticationManager authenticationManager, JwtService jwtService,
                             ProfileService profileService, MessageSource messageSource, Clock clock) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.profileService = profileService;
        this.messageSource = messageSource;
        this.clock = clock;
    }

    /**
     * Usa el mismo AuthenticationManager que el formulario web: BCrypt, bloqueo
     * por intentos fallidos y cuentas desactivadas funcionan igual en ambos canales.
     */
    @PostMapping("/login")
    @Operation(summary = "Iniciar sesion y obtener un token")
    @ApiResponse(responseCode = "200", description = "Credenciales validas")
    @ApiResponse(responseCode = "401", description = "Credenciales invalidas, cuenta bloqueada o desactivada",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));
            JornalUserDetails user = (JornalUserDetails) authentication.getPrincipal();
            return ResponseEntity.ok(new TokenResponse(jwtService.generateToken(user), "Bearer",
                    jwtService.expirationSeconds(), user.id(), user.role(), user.fullName()));
        } catch (AuthenticationException e) {
            String code = e instanceof LockedException ? "api.cuenta.bloqueada"
                    : e instanceof DisabledException ? "api.cuenta.inactiva" : "api.credenciales.invalidas";
            String message = messageSource.getMessage(code, null, code, LocaleContextHolder.getLocale());
            HttpStatus status = HttpStatus.UNAUTHORIZED;
            return ResponseEntity.status(status).body(new ApiError(OffsetDateTime.now(clock), status.value(),
                    status.getReasonPhrase(), ApiError.toApiCode(code), message, http.getRequestURI(), List.of()));
        }
    }

    @GetMapping("/me")
    @Operation(summary = "Datos de la cuenta autenticada")
    @SecurityRequirement(name = "bearerAuth")
    public ProfileSummary me(@AuthenticationPrincipal JornalUserDetails me) {
        return profileService.summary(me.id());
    }
}

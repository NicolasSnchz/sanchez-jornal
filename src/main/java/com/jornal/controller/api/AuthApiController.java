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
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticaci\u00f3n", description = "Obtener el token JWT y consultar la cuenta autenticada")
public class AuthApiController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final ProfileService profileService;

    public AuthApiController(AuthenticationManager authenticationManager, JwtService jwtService,
                             ProfileService profileService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.profileService = profileService;
    }

    /**
     * Usa el mismo AuthenticationManager que el formulario web: BCrypt, bloqueo
     * por intentos fallidos y cuentas desactivadas funcionan igual en ambos canales.
     * Si las credenciales fallan, la AuthenticationException la responde
     * ApiExceptionHandler con 401; aqui no hay try/catch.
     */
    @PostMapping("/login")
    @Operation(summary = "Iniciar sesi\u00f3n y obtener un token")
    @ApiResponse(responseCode = "200", description = "Credenciales v\u00e1lidas: token JWT y datos de la cuenta")
    @ApiResponse(responseCode = "400", description = "Correo o clave vac\u00edos o con formato inv\u00e1lido",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "Credenciales inv\u00e1lidas, cuenta bloqueada o desactivada",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));
        JornalUserDetails user = (JornalUserDetails) authentication.getPrincipal();
        return new TokenResponse(jwtService.generateToken(user), "Bearer", jwtService.expirationSeconds(),
                user.id(), user.role(), user.fullName());
    }

    @GetMapping("/me")
    @Operation(summary = "Datos de la cuenta autenticada")
    @SecurityRequirement(name = "bearerAuth")
    public ProfileSummary me(@AuthenticationPrincipal JornalUserDetails me) {
        return profileService.summary(me.id());
    }
}

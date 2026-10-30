package com.jornal.config;

import com.jornal.security.ApiErrorWriter;
import com.jornal.security.JornalUserDetailsService;
import com.jornal.security.JwtAuthFilter;
import com.jornal.security.JwtService;
import com.jornal.security.LoginFailureHandler;
import com.jornal.security.RoleBasedSuccessHandler;
import com.jornal.security.SecurityProperties;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Dos cadenas de filtros independientes:
 * <ol>
 *   <li>/api/** (orden 1): sin estado, JWT en el header Authorization, sin CSRF
 *       porque el navegador nunca adjunta ese header por su cuenta.</li>
 *   <li>Resto (orden 2): sesion con cookie para las vistas Thymeleaf, con CSRF
 *       activo porque el navegador si envia la cookie automaticamente.</li>
 * </ol>
 * La autorizacion por rol se aplica aqui por URL y otra vez en los servicios con
 * {@code @PreAuthorize} (defensa en profundidad). La propiedad de cada recurso se
 * verifica en las consultas de los repositorios.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /** CSP estricta: sin scripts ni estilos en linea y sin recursos externos. */
    private static final String STRICT_CSP = "default-src 'self'; script-src 'self'; style-src 'self'; "
            + "img-src 'self' data:; font-src 'self'; object-src 'none'; base-uri 'self'; "
            + "form-action 'self'; frame-ancestors 'none'";

    /** Swagger UI necesita estilos en linea; solo existe en desarrollo. */
    private static final String SWAGGER_CSP = "default-src 'self'; script-src 'self'; "
            + "style-src 'self' 'unsafe-inline'; img-src 'self' data:; frame-ancestors 'none'";

    @Bean
    public PasswordEncoder passwordEncoder(SecurityProperties properties) {
        return new BCryptPasswordEncoder(properties.bcryptStrength());
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /**
     * CORS solo para la API y solo para los origenes configurados (por ejemplo,
     * una app movil web). Sin configuracion, ningun sitio externo puede llamarla
     * desde un navegador.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:}") List<String> allowedOrigins) {
        CorsConfiguration api = new CorsConfiguration();
        api.setAllowedOrigins(allowedOrigins.stream().filter(origin -> !origin.isBlank()).toList());
        api.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        api.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        api.setExposedHeaders(List.of("Location"));
        api.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", api);
        return source;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http, JwtService jwtService,
                                              JornalUserDetailsService userDetailsService,
                                              ApiErrorWriter apiErrorWriter) throws Exception {
        http.securityMatcher("/api/**")
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers("/api/obras/**", "/api/frentes/**", "/api/asignaciones/**")
                        .hasRole("FOREMAN")
                        .requestMatchers("/api/mis-asignaciones/**", "/api/jornadas/**", "/api/pagos/**")
                        .hasRole("WORKER")
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(apiErrorWriter)
                        .accessDeniedHandler(apiErrorWriter))
                .addFilterBefore(new JwtAuthFilter(jwtService, userDetailsService),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        PathPatternRequestMatcher.Builder paths = PathPatternRequestMatcher.withDefaults();
        RequestMatcher swagger = new OrRequestMatcher(
                paths.matcher("/swagger-ui/**"),
                paths.matcher("/swagger-ui.html"),
                paths.matcher("/v3/api-docs/**"));
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/js/**", "/fonts/**", "/img/**", "/favicon.svg").permitAll()
                        .requestMatchers("/login", "/registro", "/registro/**", "/error", "/error/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers(swagger).permitAll()
                        .requestMatchers("/maestro/**").hasRole("FOREMAN")
                        .requestMatchers("/trabajador/**").hasRole("WORKER")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(new RoleBasedSuccessHandler())
                        .failureHandler(new LoginFailureHandler())
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?salida")
                        .deleteCookies("JSESSIONID")
                        .permitAll())
                .exceptionHandling(ex -> ex.accessDeniedPage("/error/403"))
                .headers(headers -> headers
                        .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(new NegatedRequestMatcher(swagger),
                                new StaticHeadersWriter("Content-Security-Policy", STRICT_CSP)))
                        .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(swagger,
                                new StaticHeadersWriter("Content-Security-Policy", SWAGGER_CSP)))
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.SAME_ORIGIN)));
        return http.build();
    }
}

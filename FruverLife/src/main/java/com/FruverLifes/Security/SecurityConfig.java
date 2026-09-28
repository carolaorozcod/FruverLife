package com.FruverLifes.Security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth

                        // Login y logout
                        .requestMatchers("/auth/**").permitAll()

                        // Archivos estáticos
                        .requestMatchers(
                                "/EstiloAdmi.css",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/favicon.ico"
                        ).permitAll()

                        // MENÚ ADMINISTRADOR
                        .requestMatchers(
                                "/menu/administrador",
                                "/menu/gestion",
                                "/menu/proveedores",
                                "/menu/desperdicios",
                                "/menu/mercancia",
                                "/productos/**"
                        ).hasRole("ADMINISTRADOR")

                        // MENÚ GERENTE
                        .requestMatchers(
                                "/menu/gerente",
                                "/menu/informemer",
                                "/menu/informe-diario",
                                "/menu/informedes",
                                "/menu/usuarios"
                        ).hasRole("GERENTE")

                        // MENÚ CAJERO
                        .requestMatchers(
                                "/menu/cajero",
                                "/menu/clientes",
                                "/menu/ventas",
                                "/menu/precio",
                                "/menu/catalogo"
                        ).hasRole("CAJERO")

                        // Cualquier otra ruta necesita JWT válido
                        .anyRequest().authenticated()
                )

                // Desactivar el login automático de Spring
                .formLogin(form -> form.disable())

                // Si intenta entrar sin autenticarse, volver al login
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(
                                (request, response, authException) ->
                                        response.sendRedirect("/auth/login")
                        )
                )

                // Nuestro filtro JWT se ejecuta antes del filtro de Spring
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
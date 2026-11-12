package com.FruverLifes.Security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    // Lo usan el login y el registro de usuarios
    @Bean
    @SuppressWarnings("deprecation")
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
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
                                "/login.css",
                                "/imgfondo.jpg.webp",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/favicon.ico"
                        ).permitAll()

                        // ADMINISTRADOR
                        .requestMatchers(
                                "/menu/administrador",
                                "/menu/gestion",
                                "/menu/proveedores",
                                "/menu/desperdicios",
                                "/menu/mercancia",
                                "/productos/**",
                                "/proveedores/**",
                                "/inventario/**",
                                "/desperdicios/**"
                        ).hasRole("ADMINISTRADOR")

                        // GERENTE
                        .requestMatchers(
                                "/menu/gerente",
                                "/menu/informemer",
                                "/menu/informe-diario",
                                "/menu/informedes",
                                "/menu/usuarios",
                                "/usuarios/**"
                        ).hasRole("GERENTE")

                        // CAJERO
                        .requestMatchers(
                                "/menu/cajero",
                                "/menu/clientes",
                                "/menu/ventas",
                                "/menu/precio",
                                "/menu/catalogo",
                                "/Cliente/**",
                                "/ventas/**",
                                "/verificar/**"
                        ).hasRole("CAJERO")

                        // Cualquier otra ruta necesita JWT válido
                        .anyRequest().authenticated()
                )

                .formLogin(form -> form.disable())

                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(
                                (request, response, authException) ->
                                        response.sendRedirect("/auth/login")
                        )
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
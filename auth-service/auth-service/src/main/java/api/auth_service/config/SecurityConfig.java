package api.auth_service.config;

import api.auth_service.security.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Configuration;

import org.springframework.security.authentication.AuthenticationProvider;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.context.annotation.Bean;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                // JWT REST API doesn't need CSRF
                .csrf(csrf -> csrf.disable())

                // Don't maintain HTTP sessions
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                // Configure public/protected endpoints
                .authorizeHttpRequests(auth ->
                        auth
                                // Register/login endpoints are public
                                .requestMatchers("/api/auth/**")
                                .permitAll()

                                // Everything else needs JWT authentication
                                .anyRequest()
                                .authenticated()
                )

                // Use our authentication provider
                .authenticationProvider(authenticationProvider)

                // Check JWT before Spring's normal authentication filter
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
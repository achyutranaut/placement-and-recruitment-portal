package com.placement.portal.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final List<String> allowedOrigins;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthFilter,
            @Value("${cors.allowed-origins:http://localhost:3001,http://127.0.0.1:3001,http://localhost:3000,http://127.0.0.1:3000,http://localhost:5173,http://127.0.0.1:5173}") List<String> allowedOrigins
    ) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin())) // Protect against clickjacking while allowing same-origin frames
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType("application/json");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write("{\"success\":false,\"message\":\"Full authentication is required to access this resource.\",\"data\":null}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setContentType("application/json");
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.getWriter().write("{\"success\":false,\"message\":\"Access denied: You do not have permission to access this resource.\",\"data\":null}");
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/system/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/drives/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/programs", "/api/v1/programs/**").permitAll()
                        .requestMatchers("/api/v1/companies/my").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/companies", "/api/v1/companies/**").permitAll()
                        .requestMatchers("/api/v1/drives/**").hasAnyRole("STUDENT", "RECRUITER", "ADMIN")
                        .requestMatchers("/api/v1/resumes/**").hasAnyRole("STUDENT", "RECRUITER", "ADMIN")
                        .requestMatchers("/api/v1/students/**").hasAnyRole("STUDENT", "RECRUITER", "ADMIN")
                        .requestMatchers("/api/v1/student/**").hasAnyRole("STUDENT", "ADMIN")
                        .requestMatchers("/api/v1/applications/**").hasAnyRole("STUDENT", "RECRUITER", "ADMIN")
                        .requestMatchers("/api/v1/interviews/**").hasAnyRole("STUDENT", "RECRUITER", "ADMIN")
                        .requestMatchers("/api/v1/offers/**").hasAnyRole("STUDENT", "RECRUITER", "ADMIN")
                        .requestMatchers("/api/v1/programs/**", "/api/v1/batches/**").hasAnyRole("STUDENT", "RECRUITER", "ADMIN")
                        .requestMatchers("/api/v1/reports/**").hasAnyRole("STUDENT", "RECRUITER", "ADMIN")
                        .requestMatchers("/api/v1/recruiter/**").hasAnyRole("RECRUITER", "ADMIN")
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/graph/elements").hasRole("ADMIN")
                        .requestMatchers("/api/v1/graph/sync", "/api/v1/graph/reconnect").hasRole("ADMIN")
                        .requestMatchers("/api/v1/graph/**").hasAnyRole("STUDENT", "RECRUITER", "ADMIN")
                        .requestMatchers("/api/v1/semantic-search/index", "/api/v1/semantic-search/reconnect").hasRole("ADMIN")
                        .requestMatchers("/api/v1/semantic-search/**").hasAnyRole("STUDENT", "RECRUITER", "ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With", "Access-Control-Request-Method", "Access-Control-Request-Headers"));
        config.setExposedHeaders(Arrays.asList("Authorization", "Content-Disposition"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

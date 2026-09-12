package com.placement.portal.config;

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

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
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
                .headers(headers -> headers.frameOptions(frame -> frame.disable())) // Enable H2 console
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
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
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

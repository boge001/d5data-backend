package com.d5data.demo.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername("test")
                        .password(encoder.encode("123456"))
                        .roles("USER")
                        .build());
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper mapper) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/auth/csrf", "/api/auth/login").permitAll()
                        .requestMatchers("/api/hello").authenticated()
                        .anyRequest().denyAll())
                .httpBasic(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .formLogin(form -> form
                        .loginProcessingUrl("/api/auth/login")
                        .successHandler((request, response, authentication) -> writeJson(
                                response, mapper, HttpServletResponse.SC_OK,
                                Map.of("username", authentication.getName(), "authenticated", true)))
                        .failureHandler((request, response, exception) -> writeJson(
                                response, mapper, HttpServletResponse.SC_UNAUTHORIZED,
                                Map.of("error", "Invalid username or password"))))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpServletResponse.SC_NO_CONTENT)))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> writeJson(
                                response, mapper, HttpServletResponse.SC_UNAUTHORIZED,
                                Map.of("error", "Authentication required")))
                        .accessDeniedHandler((request, response, exception) -> writeJson(
                                response, mapper, HttpServletResponse.SC_FORBIDDEN,
                                Map.of("error", "Access denied"))));

        return http.build();
    }

    private static void writeJson(
            HttpServletResponse response,
            ObjectMapper mapper,
            int status,
            Map<String, ?> body) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        mapper.writeValue(response.getOutputStream(), body);
    }
}

package com.example.whattowatchnow.config;

import com.example.whattowatchnow.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserService userService;

    @Autowired
    public SecurityConfig(UserService userService) {
        this.userService = userService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable) // Disable CSRF for stateless APIs
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/users/register", "/login").permitAll() // Permit access to registration and login
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/movies/**").permitAll() // Allow GET requests to /api/movies for all users
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/movies").authenticated() // Require authentication for POST to /api/movies
                // Review endpoints
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/movies/{movieId}/reviews").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/users/{userId}/reviews").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/movies/{movieId}/reviews").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/reviews/{reviewId}").authenticated()
                // User profile endpoints
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/users/me").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/users/me").authenticated()
                .anyRequest().authenticated() // Secure all other endpoints
            )
            .formLogin(formLogin -> formLogin // Basic form login configuration
                .loginProcessingUrl("/login") // URL to submit the username and password
                .permitAll()
            )
            .logout(logout -> logout.permitAll()); // Allow all users to logout

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.userDetailsService(userService)
                .passwordEncoder(passwordEncoder());
        return authenticationManagerBuilder.build();
    }
}

package com.example.lab3_clean;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;

import javax.sql.DataSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf((csrf) -> csrf.disable())
                .authorizeHttpRequests((requests) -> requests
                        // Дозволяємо доступ до логіну та статики всім
                        .requestMatchers("/login", "/error", "/css/**", "/js/**").permitAll()
                        // Всі інші сторінки - тільки для авторизованих
                        .anyRequest().authenticated()
                )
                .formLogin((form) -> form
                        .loginPage("/login") // Вказуємо нашу власну сторінку
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/", true) // Завжди на головну після входу
                        .permitAll()
                )
                .logout((logout) -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout") // Після виходу - на сторінку логіну
                        .deleteCookies("JSESSIONID") // Обов'язково чистимо куки
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public UserDetailsManager userDetailsManager(DataSource dataSource) {
        JdbcUserDetailsManager jdbcManager = new JdbcUserDetailsManager(dataSource);
        jdbcManager.setUsersByUsernameQuery("SELECT username, password, enabled FROM users WHERE username=?");
        jdbcManager.setAuthoritiesByUsernameQuery("SELECT username, role FROM users WHERE username=?");
        return jdbcManager;
    }

    @SuppressWarnings("deprecation")
    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }
}
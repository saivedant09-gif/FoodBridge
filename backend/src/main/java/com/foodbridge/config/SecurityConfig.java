package com.foodbridge.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import java.util.Collections;
import java.util.List;

@Configuration
@EnableWebSecurity
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
public class SecurityConfig {

    private final JdbcTemplate jdbcTemplate;

    public SecurityConfig(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new UserDetailsService() {
            @Override
            public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
                String sql = "SELECT u.user_id, u.email, u.password_hash, u.role, u.account_status FROM users u WHERE u.email = ?";
                List<UserDetails> users = jdbcTemplate.query(sql, (rs, rowNum) -> {
                    String userEmail = rs.getString("email");
                    String passwordHash = rs.getString("password_hash");
                    String role = rs.getString("role");
                    String accountStatus = rs.getString("account_status");

                    boolean enabled = !"SUSPENDED".equalsIgnoreCase(accountStatus) && !"DEACTIVATED".equalsIgnoreCase(accountStatus);
                    String roleAuthority = role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase();

                    return new User(
                            userEmail,
                            passwordHash,
                            enabled,
                            true,
                            true,
                            true,
                            Collections.singletonList(new SimpleGrantedAuthority(roleAuthority))
                    );
                }, email);

                if (users.isEmpty()) {
                    throw new UsernameNotFoundException("User not found with email: " + email);
                }
                return users.get(0);
            }
        };
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .securityContext(context -> context.securityContextRepository(securityContextRepository()))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write("{\"success\":false,\"error\":\"Unauthorized\",\"message\":\""
                                    + authException.getMessage().replace("\"", "\\\"") + "\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.getWriter().write("{\"success\":false,\"error\":\"Forbidden\",\"message\":\""
                                    + accessDeniedException.getMessage().replace("\"", "\\\"") + "\"}");
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/v1/listings", "/api/v1/listings/**", "/api/v1/reports/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/register").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/provider/**").hasRole("PROVIDER")
                        .requestMatchers("/api/v1/recipient/**").hasRole("RECIPIENT")
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}

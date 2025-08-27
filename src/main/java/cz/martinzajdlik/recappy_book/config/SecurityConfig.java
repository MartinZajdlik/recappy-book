package cz.martinzajdlik.recappy_book.config;

import cz.martinzajdlik.recappy_book.security.CustomUserDetailsService;
import cz.martinzajdlik.recappy_book.security.JwtRequestFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // !!! Pokud budeš používat cookie s JWT, nastav níž allowCredentials(true)
        config.setAllowedOrigins(List.of("https://recappy-book-official.onrender.com","http://localhost:5500", "http://127.0.0.1:5500", "http://localhost:63342"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // Je lepší být explicitní:
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        // Pokud chceš číst hlavičku Authorization na frontendu, můžeš ji “expose-nout”:
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true); // <- dej true, pokud používáš cookie s JWT

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(c -> c.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/").permitAll()
                        .requestMatchers("/actuator/health").permitAll()

                        // Auth endpoints – explicitně vypsané pro přehlednost
                        .requestMatchers("/auth/login",
                                "/auth/register",
                                "/auth/confirm",
                                "/auth/forgot",
                                "/auth/reset").permitAll()

                        // Obrázky (pokud servíruješ veřejně)
                        .requestMatchers("/pictures/**").permitAll()

                        // Veřejné čtení receptů
                        .requestMatchers(HttpMethod.GET, "/recepty/**").permitAll()

                        // Admin operace nad recepty
                        .requestMatchers(HttpMethod.POST, "/recepty/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/recepty/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/recepty/**").hasRole("ADMIN")

                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

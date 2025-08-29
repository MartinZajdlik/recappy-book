package cz.martinzajdlik.recappy_book;

import cz.martinzajdlik.recappy_book.model.User;
import cz.martinzajdlik.recappy_book.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

@SpringBootApplication
public class RecappyBookApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecappyBookApplication.class, args);
    }

    @Bean
    CommandLineRunner run(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // === FEATURE-FLAG: vypnutí seedingu přes ENV ===
            boolean seedEnabled = Boolean.parseBoolean(
                    System.getenv().getOrDefault("ADMIN_SEED_ENABLED", "true")
            );
            if (!seedEnabled) {
                System.out.println("Admin seeding vypnut (ADMIN_SEED_ENABLED=false).");
                return; // přeskoč celý seeding
            }

            String adminPassword = System.getenv("ADMIN_DEFAULT_PASSWORD");
            String userPassword = System.getenv("USER_DEFAULT_PASSWORD");

            if (adminPassword == null || adminPassword.isBlank()) {
                System.err.println("⚠️ ADMIN_DEFAULT_PASSWORD není nastaven. Admin nebude vytvořen.");
            } else if (userRepository.findByUsername("admin").isEmpty()) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode(adminPassword));
                admin.setRole("ROLE_ADMIN");
                admin.setEmail("m.zajdlik@seznam.cz");
                userRepository.save(admin);
                System.out.println("✅ Admin vytvořen.");
            }

            if (userRepository.findByUsername("user").isEmpty()) {
                if (userPassword == null || userPassword.isBlank()) {
                    userPassword = "user"; // fallback default
                }
                User user = new User();
                user.setUsername("user");
                user.setPassword(passwordEncoder.encode(userPassword));
                user.setRole("ROLE_USER");
                user.setEmail("pomocny@seznam.cz");
                userRepository.save(user);
                System.out.println("✅ User vytvořen.");
            }
        };
    }
    @Bean
    CommandLineRunner printDbInfo(DataSource dataSource) {
        return args -> {
            try (Connection conn = dataSource.getConnection()) {
                DatabaseMetaData md = conn.getMetaData();
                System.out.println("🔎 DB URL  : " + md.getURL());
                System.out.println("🔎 DB User : " + md.getUserName());
            }
        };
    }


}

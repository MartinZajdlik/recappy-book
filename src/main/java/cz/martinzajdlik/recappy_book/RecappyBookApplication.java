package cz.martinzajdlik.recappy_book;

import cz.martinzajdlik.recappy_book.model.User;
import cz.martinzajdlik.recappy_book.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class RecappyBookApplication {

	public static void main(String[] args) {
		SpringApplication.run(RecappyBookApplication.class, args);
	}

	@Bean
	CommandLineRunner createAdminIfNotExists(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			if (userRepository.findByUsername("admin").isEmpty()) {
				User admin = new User();
				admin.setUsername("admin");
				admin.setPassword(passwordEncoder.encode("admin123"));
				admin.setRole("ROLE_ADMIN");
				userRepository.save(admin);
				System.out.println("Vytvořen uživatel admin s přednastaveným heslem.");
			}
		};
	}
}

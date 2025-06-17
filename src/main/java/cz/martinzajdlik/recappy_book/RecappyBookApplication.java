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
	CommandLineRunner run(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			if (userRepository.findByUsername("admin").isEmpty()) {
				User admin = new User();
				admin.setUsername("admin");
				admin.setPassword(passwordEncoder.encode("admin")); // zašifrované heslo
				admin.setRole("ROLE_ADMIN"); // POZOR na správný název role
				userRepository.save(admin);
			}

			if (userRepository.findByUsername("user").isEmpty()) {
				User user = new User();
				user.setUsername("user");
				user.setPassword(passwordEncoder.encode("user")); // jednoduché heslo na test
				user.setRole("ROLE_USER"); // běžný uživatel
				userRepository.save(user);
			}
		};
	}

}

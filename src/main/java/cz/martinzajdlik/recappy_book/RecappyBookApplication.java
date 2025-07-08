package cz.martinzajdlik.recappy_book;

import cz.martinzajdlik.recappy_book.model.User;
import cz.martinzajdlik.recappy_book.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class RecappyBookApplication {

	// 🛡️ Načtení hesel z application.properties nebo prostředí
	@Value("${admin.default.password}")
	private String adminPassword;

	@Value("${user.default.password}")
	private String userPassword;

	public static void main(String[] args) {
		SpringApplication.run(RecappyBookApplication.class, args);
	}

	@Bean
	CommandLineRunner run(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			if (userRepository.findByUsername("admin").isEmpty()) {
				User admin = new User();
				admin.setUsername("admin");
				admin.setPassword(passwordEncoder.encode(adminPassword)); // 🧠 Heslo z proměnné
				admin.setRole("ROLE_ADMIN");
				admin.setEmail("m.zajdlik@seznam.cz");
				userRepository.save(admin);
			}

			if (userRepository.findByUsername("user").isEmpty()) {
				User user = new User();
				user.setUsername("user");
				user.setPassword(passwordEncoder.encode(userPassword)); // 🧠 Heslo z proměnné
				user.setRole("ROLE_USER");
				user.setEmail("pomocny@seznam.cz");
				userRepository.save(user);
			}
		};
	}
}

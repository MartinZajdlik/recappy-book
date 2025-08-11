package cz.martinzajdlik.recappy_book.repository;

import cz.martinzajdlik.recappy_book.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    long countByRole(String role);
}

package cz.martinzajdlik.recappy_book.controller;

import cz.martinzajdlik.recappy_book.model.User;
import cz.martinzajdlik.recappy_book.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")  // endpointy dostupné jen adminům
public class AdminUserController {

    private final UserRepository userRepository;

    @Autowired
    public AdminUserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Získat všechny uživatele
    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Změnit roli uživatele
    @PutMapping("/{id}/role")
    public ResponseEntity<String> changeUserRole(@PathVariable Long id, @RequestParam String role) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User user = userOpt.get();
        user.setRole(role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase());
        userRepository.save(user);
        return ResponseEntity.ok("Role uživatele byla změněna.");
    }

    // Smazat uživatele
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        userRepository.deleteById(id);
        return ResponseEntity.ok("Uživatel byl smazán.");
    }
}

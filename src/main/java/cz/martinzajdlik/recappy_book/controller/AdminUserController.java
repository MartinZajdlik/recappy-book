package cz.martinzajdlik.recappy_book.controller;


import cz.martinzajdlik.recappy_book.dto.UserDTO;
import cz.martinzajdlik.recappy_book.model.User;
import cz.martinzajdlik.recappy_book.repository.PasswordResetTokenRepository;
import cz.martinzajdlik.recappy_book.repository.UserRepository;
import cz.martinzajdlik.recappy_book.repository.VerificationTokenRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
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
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    public AdminUserController(
            UserRepository userRepository,
            VerificationTokenRepository verificationTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository
    ) {
        this.userRepository = userRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    // Získat všechny uživatele
    @GetMapping
    public List<UserDTO> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(user -> new UserDTO(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getRole()))
                .toList();
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
    @Transactional
    public ResponseEntity<String> deleteUser(@PathVariable Long id, Authentication auth) {
        // 1) existuje?
        User toDelete = userRepository.findById(id).orElse(null);
        if (toDelete == null) {
            return ResponseEntity.notFound().build();
        }

        // 2) neodstřel sám sebe
        String currentUsername = auth != null ? auth.getName() : null;
        if (currentUsername != null && currentUsername.equalsIgnoreCase(toDelete.getUsername())) {
            return ResponseEntity.badRequest().body("Nemůžeš smazat sám sebe.");
        }

        // 3) nenech smazat posledního admina
        if ("ROLE_ADMIN".equalsIgnoreCase(toDelete.getRole())) {
            long admins = userRepository.countByRole("ROLE_ADMIN");
            if (admins <= 1) {
                return ResponseEntity.badRequest().body("Nelze smazat posledního administrátora.");
            }
        }

        // 4) nejdřív smaž závislosti (tokeny), pak uživatele
        verificationTokenRepository.deleteByUser_Id(id);
        passwordResetTokenRepository.deleteAllByUser_Id(id);

        userRepository.deleteById(id);
        return ResponseEntity.ok("Uživatel byl smazán.");
    }

    // Aktivovat uživatele
    @PatchMapping("/{id}/enable")
    public ResponseEntity<String> enableUser(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(u -> {
                    u.setEnabled(true);
                    userRepository.save(u);
                    return ResponseEntity.ok("Uživatel byl aktivován.");
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Deaktivovat uživatele
    @PatchMapping("/{id}/disable")
    public ResponseEntity<String> disableUser(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(u -> {
                    // bezpečnost: nedeaktivuj posledního admina
                    if ("ROLE_ADMIN".equalsIgnoreCase(u.getRole())) {
                        long admins = userRepository.countByRole("ROLE_ADMIN");
                        if (admins <= 1) {
                            return ResponseEntity.badRequest().body("Nelze deaktivovat posledního administrátora.");
                        }
                    }
                    u.setEnabled(false);
                    userRepository.save(u);
                    return ResponseEntity.ok("Uživatel byl deaktivován.");
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

}

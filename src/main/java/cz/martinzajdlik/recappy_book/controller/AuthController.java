package cz.martinzajdlik.recappy_book.controller;

import cz.martinzajdlik.recappy_book.model.User;
import cz.martinzajdlik.recappy_book.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public String register(@RequestBody User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return "Uživatel již existuje.";
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("ROLE_USER");  // každý registrovaný je běžný uživatel
        userRepository.save(user);

        return "Registrace proběhla úspěšně.";

    }
    @PostMapping("/login")
    public String login(@RequestBody User user) {
        return userRepository.findByUsername(user.getUsername())
                .map(dbUser -> {
                    if (passwordEncoder.matches(user.getPassword(), dbUser.getPassword())) {
                        return "Přihlášení úspěšné. Role: " + dbUser.getRole();
                    } else {
                        return "Špatné heslo.";
                    }
                })
                .orElse("Uživatel nenalezen.");
    }

}

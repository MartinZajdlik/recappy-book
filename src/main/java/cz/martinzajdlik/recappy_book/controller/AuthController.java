package cz.martinzajdlik.recappy_book.controller;

import cz.martinzajdlik.recappy_book.model.User;
import cz.martinzajdlik.recappy_book.repository.UserRepository;
import cz.martinzajdlik.recappy_book.security.JwtUtil;  // import na JWT utilitu (musíš vytvořit)
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;   // nástroj na práci s JWT tokeny

    @Autowired
    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Uživatel již existuje.");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("ROLE_USER");  // každý registrovaný je běžný uživatel
        userRepository.save(user);

        return ResponseEntity.ok("Registrace proběhla úspěšně.");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {
        return userRepository.findByUsername(user.getUsername())
                .map(dbUser -> {
                    if (passwordEncoder.matches(user.getPassword(), dbUser.getPassword())) {
                        // Vygeneruj JWT token
                        String token = jwtUtil.generateToken(dbUser.getUsername(), dbUser.getRole());
                        // Vrať token klientovi v JSON
                        return ResponseEntity.ok(new JwtResponse(token));
                    } else {
                        return ResponseEntity.status(401).body("Špatné heslo.");
                    }
                })
                .orElse(ResponseEntity.status(404).body("Uživatel nenalezen."));
    }

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    //promazat ( delete users používát jen pro vývoj )
    @DeleteMapping("/delete-all")
    public String deleteAllUsers() {
        userRepository.deleteAll();
        return "Všichni uživatelé byli smazáni.";
    }

    // Pomocná třída pro JSON odpověď s tokenem
    public static class JwtResponse {
        private String token;

        public JwtResponse(String token) {
            this.token = token;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }
}

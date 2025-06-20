package cz.martinzajdlik.recappy_book.controller;

import cz.martinzajdlik.recappy_book.model.User;
import cz.martinzajdlik.recappy_book.repository.UserRepository;
import cz.martinzajdlik.recappy_book.security.JwtUtil;  // import na JWT utilitu (musíš vytvořit)
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:5500", allowCredentials = "true")
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
    public ResponseEntity<?> login(@RequestBody User user, HttpServletResponse response) {
        return userRepository.findByUsername(user.getUsername())
                .map(dbUser -> {
                    if (passwordEncoder.matches(user.getPassword(), dbUser.getPassword())) {
                        // Vygeneruj JWT token
                        String token = jwtUtil.generateToken(dbUser.getUsername(), dbUser.getRole());

                        // Nastav cookie s tokenem
                        Cookie cookie = new Cookie("jwt", token);
                        cookie.setHttpOnly(true);
                        cookie.setSecure(false); // na produkci true a HTTPS
                        cookie.setPath("/");
                        cookie.setMaxAge(10 * 60 * 60); // 10 hodin
                        response.addCookie(cookie);

                        HttpHeaders headers = new HttpHeaders();
                        headers.add("Authorization", "Bearer " + token);


                        // ⬇️ Nově pošleme token i v těle odpovědi
                        return ResponseEntity.ok(new JwtResponse(token));
                    } else {
                        return ResponseEntity.status(401).body("Špatné heslo.");
                    }
                })
                .orElse(ResponseEntity.status(404).body("Uživatel nenalezen."));
    }


    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("jwt", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // na produkci true a HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(0); // smaže cookie

        response.addCookie(cookie);
        return ResponseEntity.ok("Odhlášení proběhlo úspěšně.");
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

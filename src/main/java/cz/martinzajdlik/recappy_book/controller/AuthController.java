package cz.martinzajdlik.recappy_book.controller;

import cz.martinzajdlik.recappy_book.dto.UserRegistrationDTO;
import cz.martinzajdlik.recappy_book.model.User;
import cz.martinzajdlik.recappy_book.repository.UserRepository;
import cz.martinzajdlik.recappy_book.security.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<String> register(@RequestBody @Valid UserRegistrationDTO registrationDTO) {
        if (userRepository.findByUsername(registrationDTO.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Uživatel již existuje.");
        }

        User newUser = new User();
        newUser.setUsername(registrationDTO.getUsername());
        newUser.setPassword(passwordEncoder.encode(registrationDTO.getPassword()));
        newUser.setEmail(registrationDTO.getEmail());
        newUser.setRole("ROLE_USER");

        userRepository.save(newUser);

        return ResponseEntity.ok("Registrace proběhla úspěšně.");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user, HttpServletResponse response) {
        return userRepository.findByUsername(user.getUsername())
                .map(dbUser -> {
                    if (passwordEncoder.matches(user.getPassword(), dbUser.getPassword())) {

                        String token = jwtUtil.generateToken(dbUser.getUsername(), dbUser.getRole());


                        Cookie cookie = new Cookie("jwt", token);
                        cookie.setHttpOnly(true);
                        cookie.setSecure(false); // na produkci true a HTTPS
                        cookie.setPath("/");
                        cookie.setMaxAge(10 * 60 * 60); // 10 hodin
                        response.addCookie(cookie);

                        HttpHeaders headers = new HttpHeaders();
                        headers.add("Authorization", "Bearer " + token);



                        return ResponseEntity.ok(new JwtResponse(token, dbUser.getRole()));
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

    // Pomocná třída pro JSON odpověď s tokenem
    public static class JwtResponse {
        private String token;
        private String role;

        public JwtResponse(String token, String role) {
            this.token = token;
            this.role = role;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }
}

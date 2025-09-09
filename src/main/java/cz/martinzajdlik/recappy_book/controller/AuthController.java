package cz.martinzajdlik.recappy_book.controller;

import cz.martinzajdlik.recappy_book.dto.UserRegistrationDTO;
import cz.martinzajdlik.recappy_book.dto.EmailDto;
import cz.martinzajdlik.recappy_book.dto.ResetDto;
import cz.martinzajdlik.recappy_book.model.PasswordResetToken;
import cz.martinzajdlik.recappy_book.model.User;
import cz.martinzajdlik.recappy_book.model.VerificationToken;
import cz.martinzajdlik.recappy_book.repository.PasswordResetTokenRepository;
import cz.martinzajdlik.recappy_book.repository.UserRepository;
import cz.martinzajdlik.recappy_book.repository.VerificationTokenRepository;
import cz.martinzajdlik.recappy_book.security.JwtUtil;
import cz.martinzajdlik.recappy_book.service.MailService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "https://recappy-book-official.onrender.com", allowCredentials = "true")
public class AuthController {

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final MailService mailService;

    @Value("${app.frontend.baseUrl:http://localhost:5500}")
    private String frontendBase;

    @Value("${feature.email.enabled:false}")       // ⬅️ přidáno: feature flag
    private boolean emailEnabled;

    @Autowired
    public AuthController(UserRepository userRepository,
                          VerificationTokenRepository verificationTokenRepository,
                          PasswordResetTokenRepository passwordResetTokenRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil,
                          MailService mailService) {
        this.userRepository = userRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.mailService = mailService;
    }

    // ===== REGISTRACE (auto-aktivace pokud jsou e-maily vypnuté) =====
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody @Valid UserRegistrationDTO dto) {
        if (userRepository.findByUsername(dto.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Uživatel již existuje.");
        }
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("E-mail je již použit.");
        }

        User newUser = new User();
        newUser.setUsername(dto.getUsername());
        newUser.setPassword(passwordEncoder.encode(dto.getPassword()));
        newUser.setEmail(dto.getEmail());
        newUser.setRole("ROLE_USER");

        if (!emailEnabled) {
            // ⬅️ e-maily vypnuté: rovnou aktivní, nevytváříme verifikační token, neposíláme mail
            newUser.setEnabled(true);
            userRepository.save(newUser);
            return ResponseEntity.ok("Registrace hotová. Účet je aktivní – můžeš se přihlásit.");
        } else {
            // e-maily zapnuté: klasická verifikace
            newUser.setEnabled(false);
            userRepository.save(newUser);

            VerificationToken vt = new VerificationToken();
            vt.setToken(UUID.randomUUID().toString());
            vt.setUser(newUser);
            vt.setExpiresAt(LocalDateTime.now().plusHours(24));
            vt.setUsed(false);
            verificationTokenRepository.save(vt);

            String link = frontendBase + "/?verifyToken=" + vt.getToken();
            mailService.send(newUser.getEmail(), "Potvrzení registrace",
                    "<p>Ahoj, potvrď svůj účet kliknutím:</p><p><a href='" + link + "'>Potvrdit účet</a></p>");

            return ResponseEntity.ok("Registrace proběhla. Zkontroluj e-mail pro potvrzení.");
        }
    }

    // ===== LOGIN – bez potvrzení účtu nevydávej JWT =====
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user, HttpServletResponse response) {
        Optional<User> opt = userRepository.findByUsername(user.getUsername());
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body("Uživatel nenalezen.");
        }
        User dbUser = opt.get();

        if (!dbUser.isEnabled()) {
            return ResponseEntity.status(403).body("Účet není potvrzen. Zkontroluj e-mail.");
        }

        if (!passwordEncoder.matches(user.getPassword(), dbUser.getPassword())) {
            return ResponseEntity.status(401).body("Špatné heslo.");
        }

        String token = jwtUtil.generateToken(dbUser.getUsername(), dbUser.getRole());

        Cookie cookie = new Cookie("jwt", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // v produkci true a HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(10 * 60 * 60); // 10 hodin
        response.addCookie(cookie);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);

        return ResponseEntity.ok(new JwtResponse(token, dbUser.getRole()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("jwt", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // v produkci true a HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return ResponseEntity.ok("Odhlášení proběhlo úspěšně.");
    }

    // ===== Potvrzení e-mailu =====
    @GetMapping("/confirm")
    public ResponseEntity<?> confirm(@RequestParam String token) {
        VerificationToken vt = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token nenalezen"));

        if (vt.isUsed() || vt.getExpiresAt().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("Token neplatný nebo expirovaný.");
        }

        User u = vt.getUser();
        u.setEnabled(true);
        userRepository.save(u);

        vt.setUsed(true);
        verificationTokenRepository.save(vt);

        return ResponseEntity.ok("Účet potvrzen.");
    }

    // ===== Zapomenuté heslo – požadavek (PROD verze) =====
    @PostMapping("/forgot")
    public ResponseEntity<?> forgot(@RequestBody EmailDto dto) {
        userRepository.findByEmail(dto.email()).ifPresent(u -> {
            passwordResetTokenRepository.deleteAllByUser_Id(u.getId());
            PasswordResetToken pr = new PasswordResetToken();
            pr.setToken(UUID.randomUUID().toString());
            pr.setUser(u);
            pr.setExpiresAt(LocalDateTime.now().plusMinutes(30));
            passwordResetTokenRepository.save(pr);

            String link = frontendBase + "/?resetToken=" + pr.getToken();
            mailService.send(u.getEmail(), "Reset hesla",
                    "<p>Požádal(a) jsi o reset hesla.</p><p><a href='" + link + "'>Nastavit nové heslo</a></p>");
        });
        // vždy vracej OK (neprozrazujeme, zda e-mail existuje)
        return ResponseEntity.ok().build();
    }




    // ===== Reset hesla – nastavení nového =====
    @PostMapping("/reset")
    public ResponseEntity<?> reset(@RequestBody ResetDto dto) {
        PasswordResetToken pr = passwordResetTokenRepository.findByToken(dto.token())
                .orElseThrow(() -> new IllegalArgumentException("Token nenalezen"));

        if (pr.isUsed() || pr.getExpiresAt().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("Token neplatný nebo expirovaný.");
        }

        User u = pr.getUser();
        u.setPassword(passwordEncoder.encode(dto.newPassword()));
        userRepository.save(u);

        pr.setUsed(true);
        passwordResetTokenRepository.save(pr);

        passwordResetTokenRepository.deleteAllByUser_Id(u.getId());

        return ResponseEntity.ok().build();
    }

    // ===== Informace o uživateli =====
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(org.springframework.security.core.Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Neautorizováno");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof cz.martinzajdlik.recappy_book.security.CustomUserDetails userDetails) {
            return ResponseEntity.ok(new UserInfoResponse(
                    userDetails.getUsername(),
                    userDetails.getAuthorities().iterator().next().getAuthority()
            ));
        }
        return ResponseEntity.status(401).body("Neplatný token");
    }

    // ===== Pomocné odpovědi =====
    public static class UserInfoResponse {
        private String username;
        private String role;
        public UserInfoResponse(String username, String role) {
            this.username = username; this.role = role;
        }
        public String getUsername() { return username; }
        public String getRole() { return role; }
    }

    public static class JwtResponse {
        private String token;
        private String role;
        public JwtResponse(String token, String role) { this.token = token; this.role = role; }
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }
}

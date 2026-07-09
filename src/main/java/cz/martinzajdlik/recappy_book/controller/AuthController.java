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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import cz.martinzajdlik.recappy_book.repository.RecipeRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;

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
    private final RecipeRepository recipeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final MailService mailService;

    @Value("${app.backend.baseUrl:http://localhost:8080}")
    private String backendBaseUrl;

    @Value("${feature.email.enabled:false}")       // ⬅️ přidáno: feature flag
    private boolean emailEnabled;

    @Autowired
    public AuthController(UserRepository userRepository,
                          VerificationTokenRepository verificationTokenRepository,
                          PasswordResetTokenRepository passwordResetTokenRepository,
                          RecipeRepository recipeRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil,
                          MailService mailService) {
        this.userRepository = userRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.recipeRepository = recipeRepository;
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
        newUser.setEmail(dto.getEmail().trim().toLowerCase());
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

            String link = "https://recappy-book.onrender.com/auth/confirm?token=" + vt.getToken();
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

    @DeleteMapping("/me")
    @Transactional
    public ResponseEntity<?> deleteMyAccount(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Neautorizováno.");
        }

        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Uživatel nenalezen"));

        verificationTokenRepository.deleteByUser_Id(user.getId());
        passwordResetTokenRepository.deleteAllByUser_Id(user.getId());
        recipeRepository.deleteByAuthor_Id(user.getId());

        userRepository.delete(user);

        return ResponseEntity.ok("Účet a všechny recepty byly smazány.");
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

    @PostMapping("/forgot")
    public ResponseEntity<?> forgot(@RequestBody(required = false) EmailDto dto) {
        if (dto == null || dto.email() == null || dto.email().isBlank()) {
            return ResponseEntity.badRequest().body("E-mail je povinný.");
        }

        final String raw = dto.email().trim();
        final String normalized = raw.toLowerCase(); // prevence „Admin@…“ vs „admin@…“

        var userOpt = userRepository.findByEmail(normalized);
        if (userOpt.isEmpty()) {
            // Neprozrazujeme, ale do logu si to napiš
            System.out.println("[/auth/forgot] E-mail v DB nenalezen: " + normalized);
            return ResponseEntity.ok().build();
        }

        var u = userOpt.get();
        passwordResetTokenRepository.deleteAllByUser_Id(u.getId());

        PasswordResetToken pr = new PasswordResetToken();
        pr.setToken(UUID.randomUUID().toString());
        pr.setUser(u);
        pr.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        passwordResetTokenRepository.save(pr);

        String link = backendBaseUrl + "/auth/reset?token=" + pr.getToken();

        try {
            mailService.send(u.getEmail(), "Reset hesla",
                    "<p>Požádal(a) jsi o reset hesla.</p><p><a href='" + link + "'>Nastavit nové heslo</a></p>");
            System.out.println("[/auth/forgot] Reset e-mail odeslán na: " + u.getEmail() + " link=" + link);
        } catch (Exception e) {
            // Uvidíš případné chyby odesílání rovnou v logu Renderu
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Odeslání e-mailu selhalo.");
        }

        // --- Nepovinné: v DEV můžeš vracet link přímo v těle (usnadní testování)
        // return ResponseEntity.ok(link);

        return ResponseEntity.ok().build();
    }





    // ===== Reset hesla – stránka s formulářem (odkaz z e-mailu) =====
    @GetMapping(value = "/reset", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> resetPasswordPage(@RequestParam String token) {
        String safeToken = HtmlUtils.htmlEscape(token);
        String html = """
                <!DOCTYPE html>
                <html lang="cs">
                <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Reset hesla – RecAPPy BOOK</title>
                <style>
                    body { font-family: 'Segoe UI', sans-serif; background:#101021; color:#f0f0f0; display:flex; justify-content:center; padding:48px 16px; margin:0; }
                    .card { max-width:360px; width:100%%; background:#19182b; padding:24px; border-radius:12px; }
                    h1 { font-size:20px; margin:0 0 16px; }
                    input { width:100%%; padding:10px; margin:8px 0; border-radius:6px; border:1px solid #333; background:#101021; color:#f0f0f0; box-sizing:border-box; font-size:16px; }
                    button { width:100%%; padding:10px; margin-top:12px; border:none; border-radius:6px; background:#7dff50; color:#101021; font-weight:bold; cursor:pointer; font-size:16px; }
                    #msg { margin-top:12px; font-size:14px; }
                </style>
                </head>
                <body>
                <div class="card">
                    <h1>Nastavit nové heslo</h1>
                    <form id="f" data-token="%s">
                        <input type="password" id="p1" placeholder="Nové heslo" required minlength="4" autocomplete="new-password">
                        <input type="password" id="p2" placeholder="Zopakuj heslo" required minlength="4" autocomplete="new-password">
                        <button type="submit">Uložit</button>
                    </form>
                    <div id="msg"></div>
                </div>
                <script>
                    document.getElementById('f').addEventListener('submit', async function (e) {
                        e.preventDefault();
                        var p1 = document.getElementById('p1').value;
                        var p2 = document.getElementById('p2').value;
                        var msg = document.getElementById('msg');
                        if (p1 !== p2) { msg.textContent = 'Hesla se neshodují.'; return; }
                        try {
                            var res = await fetch('/auth/reset', {
                                method: 'POST',
                                headers: { 'Content-Type': 'application/json' },
                                body: JSON.stringify({ token: this.dataset.token, newPassword: p1 })
                            });
                            if (res.ok) {
                                msg.textContent = 'Heslo bylo úspěšně změněno. Teď se můžeš přihlásit v appce.';
                                document.getElementById('f').style.display = 'none';
                            } else {
                                msg.textContent = 'Odkaz je neplatný nebo expirovaný.';
                            }
                        } catch (err) {
                            msg.textContent = 'Něco se nepovedlo, zkus to znovu.';
                        }
                    });
                </script>
                </body>
                </html>
                """.formatted(safeToken);
        return ResponseEntity.ok(html);
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

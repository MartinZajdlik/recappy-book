package cz.martinzajdlik.recappy_book.controller;

import cz.martinzajdlik.recappy_book.dto.UserRegistrationDTO;
import cz.martinzajdlik.recappy_book.dto.EmailDto;
import cz.martinzajdlik.recappy_book.dto.RefreshRequest;
import cz.martinzajdlik.recappy_book.dto.ResetDto;
import cz.martinzajdlik.recappy_book.model.PasswordResetToken;
import cz.martinzajdlik.recappy_book.model.User;
import cz.martinzajdlik.recappy_book.model.VerificationToken;
import cz.martinzajdlik.recappy_book.repository.PasswordResetTokenRepository;
import cz.martinzajdlik.recappy_book.repository.RefreshTokenRepository;
import cz.martinzajdlik.recappy_book.repository.UserRepository;
import cz.martinzajdlik.recappy_book.repository.VerificationTokenRepository;
import cz.martinzajdlik.recappy_book.security.InvalidRefreshTokenException;
import cz.martinzajdlik.recappy_book.security.JwtUtil;
import cz.martinzajdlik.recappy_book.service.MailService;
import cz.martinzajdlik.recappy_book.service.RefreshTokenService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
public class AuthController {

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RecipeRepository recipeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final MailService mailService;

    @Value("${app.backend.baseUrl:http://localhost:8080}")
    private String backendBaseUrl;

    @Value("${feature.email.enabled:false}")       // ⬅️ přidáno: feature flag
    private boolean emailEnabled;

    @Autowired
    public AuthController(UserRepository userRepository,
                          VerificationTokenRepository verificationTokenRepository,
                          PasswordResetTokenRepository passwordResetTokenRepository,
                          RefreshTokenRepository refreshTokenRepository,
                          RecipeRepository recipeRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil,
                          RefreshTokenService refreshTokenService,
                          MailService mailService) {
        this.userRepository = userRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.recipeRepository = recipeRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
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

            String link = backendBaseUrl + "/auth/confirm?token=" + vt.getToken();
            mailService.send(newUser.getEmail(), "Potvrzení registrace",
                    "<p>Ahoj, potvrď svůj účet kliknutím:</p><p><a href='" + link + "'>Potvrdit účet</a></p>");

            return ResponseEntity.ok("Registrace proběhla. Zkontroluj e-mail pro potvrzení.");
        }
    }

    // ===== LOGIN – bez potvrzení účtu nevydávej JWT =====
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {
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
        String refreshToken = refreshTokenService.issue(dbUser);

        return ResponseEntity.ok(new JwtResponse(token, dbUser.getRole(), refreshToken));
    }

    // ===== Obnovení access tokenu pomocí refresh tokenu (rotace + detekce znovupoužití) =====
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest dto) {
        if (dto == null || dto.refreshToken() == null || dto.refreshToken().isBlank()) {
            return ResponseEntity.badRequest().body("Refresh token je povinný.");
        }

        try {
            RefreshTokenService.RotationResult result = refreshTokenService.rotate(dto.refreshToken());
            User user = result.user;
            String newAccessToken = jwtUtil.generateToken(user.getUsername(), user.getRole());

            return ResponseEntity.ok(new JwtResponse(newAccessToken, user.getRole(), result.rawToken));
        } catch (InvalidRefreshTokenException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody(required = false) RefreshRequest dto) {
        if (dto != null && dto.refreshToken() != null && !dto.refreshToken().isBlank()) {
            refreshTokenService.revoke(dto.refreshToken());
        }
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
        refreshTokenRepository.deleteAllByUser_Id(user.getId());
        recipeRepository.deleteByAuthor_Id(user.getId());

        userRepository.delete(user);

        return ResponseEntity.ok("Účet a všechny recepty byly smazány.");
    }

    // ===== Potvrzení e-mailu (odkaz z registračního mailu) =====
    @GetMapping(value = "/confirm", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> confirm(@RequestParam String token) {
        boolean success;
        String message;

        Optional<VerificationToken> vtOpt = verificationTokenRepository.findByToken(token);
        if (vtOpt.isEmpty()) {
            success = false;
            message = "Odkaz je neplatný.";
        } else {
            VerificationToken vt = vtOpt.get();
            if (vt.isUsed() || vt.getExpiresAt().isBefore(LocalDateTime.now())) {
                success = false;
                message = "Odkaz je neplatný nebo expirovaný.";
            } else {
                User u = vt.getUser();
                u.setEnabled(true);
                userRepository.save(u);

                vt.setUsed(true);
                verificationTokenRepository.save(vt);

                success = true;
                message = "Účet byl úspěšně potvrzen. Teď se můžeš přihlásit v appce.";
            }
        }

        String html = """
                <!DOCTYPE html>
                <html lang="cs">
                <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Potvrzení registrace – RecAPPy BOOK</title>
                <style>
                    body { font-family: 'Segoe UI', sans-serif; background:#101021; color:#f0f0f0; display:flex; justify-content:center; padding:48px 16px; margin:0; }
                    .card { max-width:360px; width:100%%; background:#19182b; padding:24px; border-radius:12px; text-align:center; }
                    h1 { font-size:20px; margin:0 0 16px; }
                    p { font-size:14px; color:%s; }
                </style>
                </head>
                <body>
                <div class="card">
                    <h1>%s</h1>
                    <p>%s</p>
                </div>
                </body>
                </html>
                """.formatted(success ? "#7dff50" : "#ff6b6b", success ? "Hotovo!" : "Něco nesedí", message);

        return ResponseEntity.ok(html);
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
        private String refreshToken;
        public JwtResponse(String token, String role, String refreshToken) {
            this.token = token; this.role = role; this.refreshToken = refreshToken;
        }
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    }
}

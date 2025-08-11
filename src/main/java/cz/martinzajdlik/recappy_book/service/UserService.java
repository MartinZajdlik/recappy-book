package cz.martinzajdlik.recappy_book.service;

import cz.martinzajdlik.recappy_book.dto.RegisterDto;
import cz.martinzajdlik.recappy_book.model.PasswordResetToken;
import cz.martinzajdlik.recappy_book.model.User;
import cz.martinzajdlik.recappy_book.model.VerificationToken;
import cz.martinzajdlik.recappy_book.repository.PasswordResetTokenRepository;
import cz.martinzajdlik.recappy_book.repository.UserRepository;
import cz.martinzajdlik.recappy_book.repository.VerificationTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository users;
    private final VerificationTokenRepository verTokens;
    private final PasswordResetTokenRepository resetTokens;
    private final PasswordEncoder encoder;
    private final MailService mail;

    @Value("${app.frontend.baseUrl}")
    private String frontendBase;

    public UserService(UserRepository users,
                       VerificationTokenRepository verTokens,
                       PasswordResetTokenRepository resetTokens,
                       PasswordEncoder encoder,
                       MailService mail) {
        this.users = users;
        this.verTokens = verTokens;
        this.resetTokens = resetTokens;
        this.encoder = encoder;
        this.mail = mail;
    }

    @Transactional
    public void register(RegisterDto dto) {
        User u = new User();
        u.setUsername(dto.username());
        u.setEmail(dto.email());
        u.setPassword(encoder.encode(dto.password()));
        u.setEnabled(false);
        users.save(u);

        VerificationToken t = new VerificationToken();
        t.setToken(UUID.randomUUID().toString());
        t.setUser(u);
        t.setExpiresAt(LocalDateTime.now().plusHours(24));
        verTokens.save(t);

        String link = frontendBase + "/?verifyToken=" + t.getToken();
        mail.send(u.getEmail(), "Potvrzení registrace",
                "<p>Ahoj, potvrď svůj účet kliknutím:</p>" +
                        "<p><a href='" + link + "'>Potvrdit účet</a></p>");
    }

    @Transactional
    public void confirm(String token) {
        VerificationToken t = verTokens.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token nenalezen"));

        if (t.isUsed() || t.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Token neplatný");
        }

        User u = t.getUser();
        u.setEnabled(true);
        users.save(u);

        t.setUsed(true);
        verTokens.save(t);
    }

    @Transactional
    public void requestPasswordReset(String email) {
        users.findByEmail(email).ifPresent(u -> {
            PasswordResetToken t = new PasswordResetToken();
            t.setToken(UUID.randomUUID().toString());
            t.setUser(u);
            t.setExpiresAt(LocalDateTime.now().plusMinutes(30));
            resetTokens.save(t);

            String link = frontendBase + "/?resetToken=" + t.getToken();
            mail.send(u.getEmail(), "Reset hesla",
                    "<p>Požádal(a) jsi o reset hesla.</p>" +
                            "<p><a href='" + link + "'>Nastavit nové heslo</a></p>");
        });
        // vždy OK – neprozrazujeme, zda e-mail existuje
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken t = resetTokens.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token nenalezen"));

        if (t.isUsed() || t.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Token neplatný");
        }

        User u = t.getUser();
        u.setPassword(encoder.encode(newPassword));
        users.save(u);

        t.setUsed(true);
        resetTokens.save(t);
    }
}

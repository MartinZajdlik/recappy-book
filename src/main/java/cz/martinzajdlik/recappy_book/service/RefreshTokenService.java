package cz.martinzajdlik.recappy_book.service;

import cz.martinzajdlik.recappy_book.model.RefreshToken;
import cz.martinzajdlik.recappy_book.model.User;
import cz.martinzajdlik.recappy_book.repository.RefreshTokenRepository;
import cz.martinzajdlik.recappy_book.security.InvalidRefreshTokenException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

// Refresh token se v DB drží jen jako SHA-256 hash (stejná logika jako u hesel) –
// jde o dlouhodobý (30 dní) vysoce citlivý credential, na rozdíl od krátkodobých
// verification/reset tokenů, které se ukládají v plaintextu.
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.jwt.refreshExpirationMillis:2592000000}") // default 30 dní
    private long refreshExpirationMillis;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public String issue(User user) {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTokenHash(hash(rawToken));
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(LocalDateTime.now().plusNanos(refreshExpirationMillis * 1_000_000L));
        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    public static class RotationResult {
        public final String rawToken;
        public final User user;

        public RotationResult(String rawToken, User user) {
            this.rawToken = rawToken;
            this.user = user;
        }
    }

    public RotationResult rotate(String rawToken) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new InvalidRefreshTokenException("Neplatný refresh token."));

        if (stored.isRevoked()) {
            // Token už byl jednou rotován/odvolán a přesto se znovu použil – signál možné
            // krádeže, proto se preventivně odvolají všechny refresh tokeny daného uživatele.
            revokeAllForUser(stored.getUser());
            throw new InvalidRefreshTokenException("Refresh token byl již použit, přihlas se prosím znovu.");
        }

        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidRefreshTokenException("Refresh token expiroval, přihlas se prosím znovu.");
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        String newRawToken = issue(stored.getUser());
        return new RotationResult(newRawToken, stored.getUser());
    }

    public void revoke(String rawToken) {
        Optional<RefreshToken> stored = refreshTokenRepository.findByTokenHash(hash(rawToken));
        stored.ifPresent(t -> {
            t.setRevoked(true);
            refreshTokenRepository.save(t);
        });
    }

    public void revokeAllForUser(User user) {
        refreshTokenRepository.deleteAllByUser_Id(user.getId());
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}

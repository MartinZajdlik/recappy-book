package cz.martinzajdlik.recappy_book.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Zablokování jednoho uživatele druhým. Čistě soukromé rozhodnutí toho,
 * kdo blokuje – recepty zablokovaného autora se pak blokujícímu přestanou
 * zobrazovat ve veřejném výpisu (viz RecipeController). Admin do toho
 * nezasahuje.
 */
@Entity
@Table(
        name = "blocked_users",
        uniqueConstraints = @UniqueConstraint(columnNames = {"blocker_id", "blocked_id"})
)
public class BlockedUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_id", nullable = false)
    private User blocker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_id", nullable = false)
    private User blocked;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public BlockedUser() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getBlocker() { return blocker; }
    public void setBlocker(User blocker) { this.blocker = blocker; }

    public User getBlocked() { return blocked; }
    public void setBlocked(User blocked) { this.blocked = blocked; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

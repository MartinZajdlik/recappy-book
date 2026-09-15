package cz.martinzajdlik.recappy_book.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Nahlášení konkrétního receptu uživatelem. Admin nahlášené recepty vidí
 * v sekci "Nahlášené recepty" a buď je ponechá (nahlášení se označí jako
 * vyřízené), nebo recept smaže – viz AdminRecipeReportController.
 */
@Entity
@Table(name = "recipe_reports")
public class RecipeReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by_id", nullable = false)
    private User reportedBy;

    @Column(nullable = false)
    private boolean resolved = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public RecipeReport() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Recipe getRecipe() { return recipe; }
    public void setRecipe(Recipe recipe) { this.recipe = recipe; }

    public User getReportedBy() { return reportedBy; }
    public void setReportedBy(User reportedBy) { this.reportedBy = reportedBy; }

    public boolean isResolved() { return resolved; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

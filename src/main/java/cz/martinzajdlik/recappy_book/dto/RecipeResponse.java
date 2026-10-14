package cz.martinzajdlik.recappy_book.dto;

import cz.martinzajdlik.recappy_book.model.Recipe;
import cz.martinzajdlik.recappy_book.model.RecipeStatus;
import cz.martinzajdlik.recappy_book.model.User;
import cz.martinzajdlik.recappy_book.service.RejectedRecipeCleanupService;

import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public class RecipeResponse {

    private Long id;
    private Long authorId;
    private String title;
    private String ingredients;
    private String instructions;
    private String category;
    private String imageUrl;
    private String authorUsername;
    private boolean favorite;
    private String status;
    private String rejectionReason;
    // Den (yyyy-MM-dd, česká časová zóna), kdy bude zamítnutý recept smazán, pokud ho autor neupraví.
    private String scheduledDeletionDate;

    public RecipeResponse(Recipe recipe, User currentUser) {
        this.id = recipe.getId();
        this.title = recipe.getTitle();
        this.ingredients = recipe.getIngredients();
        this.instructions = recipe.getInstructions();
        this.category = recipe.getCategory();
        this.imageUrl = recipe.getImageUrl();
        this.authorUsername = recipe.getAuthorUsername();
        this.authorId = recipe.getAuthor() != null ? recipe.getAuthor().getId() : null;

        this.favorite = currentUser != null
                && currentUser.getFavoriteRecipes().contains(recipe);

        this.status = recipe.getStatus() != null
                ? recipe.getStatus().name()
                : RecipeStatus.APPROVED.name();

        // Důvod posíláme jen u zamítnutých receptů – u ostatních nemá smysl.
        this.rejectionReason = recipe.getStatus() == RecipeStatus.REJECTED
                ? recipe.getRejectionReason()
                : null;

        this.scheduledDeletionDate = recipe.getStatus() == RecipeStatus.REJECTED && recipe.getRejectedAt() != null
                ? recipe.getRejectedAt()
                    .plus(RejectedRecipeCleanupService.RETENTION_DAYS, ChronoUnit.DAYS)
                    .atZone(ZoneId.of("Europe/Prague"))
                    .toLocalDate()
                    .toString()
                : null;
    }

    public Long getId() { return id; }
    public Long getAuthorId() { return authorId; }
    public String getTitle() { return title; }
    public String getIngredients() { return ingredients; }
    public String getInstructions() { return instructions; }
    public String getCategory() { return category; }
    public String getImageUrl() { return imageUrl; }
    public String getAuthorUsername() { return authorUsername; }
    public boolean isFavorite() { return favorite; }
    public String getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }
    public String getScheduledDeletionDate() { return scheduledDeletionDate; }
}
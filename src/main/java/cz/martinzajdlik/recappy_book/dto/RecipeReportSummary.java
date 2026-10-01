package cz.martinzajdlik.recappy_book.dto;

import cz.martinzajdlik.recappy_book.model.Recipe;

import java.util.List;

public class RecipeReportSummary {

    private Long id;
    private String title;
    private String ingredients;
    private String instructions;
    private String category;
    private String imageUrl;
    private String authorUsername;
    private long reportCount;
    private List<Reporter> reporters;

    /**
     * Kdo a kdy recept nahlásil (jen nevyřízená nahlášení).
     * reportedAt = epoch milisekundy, aby se v iOS snadno převedlo na Date.
     */
    public static class Reporter {
        private final String username;
        private final long reportedAt;

        public Reporter(String username, long reportedAt) {
            this.username = username;
            this.reportedAt = reportedAt;
        }

        public String getUsername() { return username; }
        public long getReportedAt() { return reportedAt; }
    }

    public RecipeReportSummary(Recipe recipe, long reportCount) {
        this(recipe, reportCount, List.of());
    }

    public RecipeReportSummary(Recipe recipe, long reportCount, List<Reporter> reporters) {
        this.id = recipe.getId();
        this.title = recipe.getTitle();
        this.ingredients = recipe.getIngredients();
        this.instructions = recipe.getInstructions();
        this.category = recipe.getCategory();
        this.imageUrl = recipe.getImageUrl();
        this.authorUsername = recipe.getAuthorUsername();
        this.reportCount = reportCount;
        this.reporters = reporters;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getIngredients() { return ingredients; }
    public String getInstructions() { return instructions; }
    public String getCategory() { return category; }
    public String getImageUrl() { return imageUrl; }
    public String getAuthorUsername() { return authorUsername; }
    public long getReportCount() { return reportCount; }
    public List<Reporter> getReporters() { return reporters; }
}

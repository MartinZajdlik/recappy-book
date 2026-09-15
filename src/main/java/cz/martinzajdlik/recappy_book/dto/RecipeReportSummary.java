package cz.martinzajdlik.recappy_book.dto;

import cz.martinzajdlik.recappy_book.model.Recipe;

public class RecipeReportSummary {

    private Long id;
    private String title;
    private String ingredients;
    private String instructions;
    private String category;
    private String imageUrl;
    private String authorUsername;
    private long reportCount;

    public RecipeReportSummary(Recipe recipe, long reportCount) {
        this.id = recipe.getId();
        this.title = recipe.getTitle();
        this.ingredients = recipe.getIngredients();
        this.instructions = recipe.getInstructions();
        this.category = recipe.getCategory();
        this.imageUrl = recipe.getImageUrl();
        this.authorUsername = recipe.getAuthorUsername();
        this.reportCount = reportCount;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getIngredients() { return ingredients; }
    public String getInstructions() { return instructions; }
    public String getCategory() { return category; }
    public String getImageUrl() { return imageUrl; }
    public String getAuthorUsername() { return authorUsername; }
    public long getReportCount() { return reportCount; }
}

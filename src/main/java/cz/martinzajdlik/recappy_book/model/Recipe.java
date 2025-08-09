package cz.martinzajdlik.recappy_book.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Název receptu nesmí být prázdný")
    @Column(nullable = false, length = 255)
    private String title;

    @NotBlank(message = "Ingredience nesmí být prázdné")
    @Column(columnDefinition = "TEXT")
    private String ingredients;

    @NotBlank(message = "Postup nesmí být prázdný")
    @Column(columnDefinition = "TEXT")
    private String instructions;

    @NotBlank(message = "Kategorie nesmí být prázdná")
    @Column(nullable = false, length = 100)
    private String category;

    @Column(name = "image_path", length = 512)
    private String imagePath;

    public Recipe() {}

    public Recipe(String title, String category, String ingredients, String instructions) {
        this.title = title;
        this.category = category;
        this.ingredients = ingredients;
        this.instructions = instructions;
    }

    // --- Gettery a settery ---
    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getIngredients() {
        return ingredients;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setIngredients(String ingredients) {
        this.ingredients = ingredients;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getImagePath() {return imagePath; }

    public void setImagePath(String imagePath) {this.imagePath = imagePath; }

}

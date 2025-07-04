package cz.martinzajdlik.recappy_book.controller;

import cz.martinzajdlik.recappy_book.model.Recipe;
import cz.martinzajdlik.recappy_book.repository.RecipeRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/recepty")
public class RecipeController {

    private final RecipeRepository recipeRepository;

    @Autowired
    public RecipeController(RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    //  Jen pro admina
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Recipe> createRecipe(@Valid @RequestBody Recipe recipe) {
        Recipe saved = recipeRepository.save(recipe);
        return ResponseEntity.ok(saved);
    }


    @GetMapping
    public List<Recipe> getRecipesByCategory(@RequestParam(required = false) String category) {
        if (category == null || category.isEmpty()) {
            return recipeRepository.findAll();
        } else {
            return recipeRepository.findByCategory(category);
        }
    }


    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @GetMapping("/{id}")
    public Optional<Recipe> getRecipeById(@PathVariable Long id) {
        return recipeRepository.findById(id);
    }

    @GetMapping("/categories")
    public List<String> getAllCategories() {
        return recipeRepository.findDistinctCategories();
    }


    // Jen admin může upravovat
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public Recipe updateRecipe(@PathVariable Long id, @Valid @RequestBody Recipe updatedRecipe) {
        return recipeRepository.findById(id)
                .map(recipe -> {
                    recipe.setTitle(updatedRecipe.getTitle());
                    recipe.setCategory(updatedRecipe.getCategory());
                    recipe.setIngredients(updatedRecipe.getIngredients());
                    recipe.setInstructions(updatedRecipe.getInstructions());
                    return recipeRepository.save(recipe);
                })
                .orElseGet(() -> {
                    updatedRecipe.setId(id);
                    return recipeRepository.save(updatedRecipe);
                });
    }

    // Jen admin může mazat
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public void deleteRecipe(@PathVariable Long id) {
        recipeRepository.deleteById(id);
    }
}

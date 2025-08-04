package cz.martinzajdlik.recappy_book.controller;

import cz.martinzajdlik.recappy_book.model.Recipe;
import cz.martinzajdlik.recappy_book.repository.RecipeRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/recepty")
public class RecipeController {

    private final RecipeRepository recipeRepository;

    @Autowired
    public RecipeController(RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    // Jen pro admina
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Recipe> createRecipe(@Valid @RequestBody Recipe recipe) {
        Recipe saved = recipeRepository.save(recipe);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/{id}/image")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> uploadImage(
            @PathVariable Long id,
            @RequestParam("image") MultipartFile imageFile) throws IOException {

        Optional<Recipe> recipeOpt = recipeRepository.findById(id);
        if (recipeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Recipe recipe = recipeOpt.get();

        String folder = "pictures/";
        String filename = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();

        File dir = new File(folder);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        Path filePath = Paths.get(folder, filename);
        Files.write(filePath, imageFile.getBytes());

        recipe.setImagePath(filename);
        recipeRepository.save(recipe);

        return ResponseEntity.ok("Obrázek uložen");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Recipe> updateRecipe(
            @PathVariable Long id,
            @RequestPart("title") String title,
            @RequestPart("category") String category,
            @RequestPart("ingredients") String ingredients,
            @RequestPart("instructions") String instructions,
            @RequestPart(value = "image", required = false) MultipartFile imageFile
    ) throws IOException {
        Optional<Recipe> recipeOpt = recipeRepository.findById(id);
        if (recipeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Recipe recipe = recipeOpt.get();

        recipe.setTitle(title);
        recipe.setCategory(category);
        recipe.setIngredients(ingredients);
        recipe.setInstructions(instructions);

        if (imageFile != null && !imageFile.isEmpty()) {
            String folder = "pictures/";
            String filename = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();
            File dir = new File(folder);
            if (!dir.exists()) dir.mkdirs();
            Path path = Paths.get(folder + filename);
            Files.write(path, imageFile.getBytes());

            recipe.setImagePath(filename);
        }

        Recipe updated = recipeRepository.save(recipe);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/random")
    public ResponseEntity<Recipe> getRandomRecipe() {
        List<Recipe> allRecipes = recipeRepository.findAll();
        if (allRecipes.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        Recipe randomRecipe = allRecipes.get(new Random().nextInt(allRecipes.size()));
        return ResponseEntity.ok(randomRecipe);
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

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public void deleteRecipe(@PathVariable Long id) {
        recipeRepository.deleteById(id);
    }
}

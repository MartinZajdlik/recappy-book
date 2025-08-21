package cz.martinzajdlik.recappy_book.controller;

import cz.martinzajdlik.recappy_book.model.Recipe;
import cz.martinzajdlik.recappy_book.repository.RecipeRepository;
import org.springframework.http.MediaType;
import cz.martinzajdlik.recappy_book.service.ImageStorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Random;


@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/recepty")
public class RecipeController {

    private final RecipeRepository recipeRepository;
    private final ImageStorageService imageStorageService;

    public RecipeController(RecipeRepository recipeRepository,
                            ImageStorageService imageStorageService) {
        this.recipeRepository = recipeRepository;
        this.imageStorageService = imageStorageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Recipe> createRecipe(
            @RequestParam("title") String title,
            @RequestParam("category") String category,
            @RequestParam("ingredients") String ingredients,
            @RequestParam("instructions") String instructions,
            @RequestParam(value = "image", required = false) MultipartFile imageFile
    ) throws IOException {
        Recipe recipe = new Recipe();
        recipe.setTitle(title);
        recipe.setCategory(category);
        recipe.setIngredients(ingredients);
        recipe.setInstructions(instructions);

        if (imageFile != null && !imageFile.isEmpty()) {
            String imageUrl = imageStorageService.upload(imageFile);
            recipe.setImageUrl(imageUrl);
        }

        Recipe saved = recipeRepository.save(recipe);
        return ResponseEntity.ok(saved);
    }

    @PostMapping(path = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> uploadImage(
            @PathVariable Long id,
            @RequestParam("image") MultipartFile imageFile) throws IOException {

        Optional<Recipe> recipeOpt = recipeRepository.findById(id);
        if (recipeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Recipe recipe = recipeOpt.get();

        String imageUrl = imageStorageService.upload(imageFile);
        recipe.setImageUrl(imageUrl);

        recipeRepository.save(recipe);

        return ResponseEntity.ok("Obrázek uložen");
    }

    @PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Recipe> updateRecipe(
            @PathVariable Long id,
            @RequestParam("title") String title,
            @RequestParam("category") String category,
            @RequestParam("ingredients") String ingredients,
            @RequestParam("instructions") String instructions,
            @RequestParam(value = "image", required = false) MultipartFile imageFile
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
            String imageUrl = imageStorageService.upload(imageFile);
            recipe.setImageUrl(imageUrl);

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

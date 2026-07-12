package cz.martinzajdlik.recappy_book.controller;

import cz.martinzajdlik.recappy_book.model.Recipe;
import cz.martinzajdlik.recappy_book.repository.RecipeRepository;
import cz.martinzajdlik.recappy_book.repository.UserRepository;
import org.springframework.http.MediaType;
import cz.martinzajdlik.recappy_book.service.ImageStorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import cz.martinzajdlik.recappy_book.model.User;
import org.springframework.security.core.Authentication;
import cz.martinzajdlik.recappy_book.dto.RecipeResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/recepty")
public class RecipeController {

    private final RecipeRepository recipeRepository;
    private final ImageStorageService imageStorageService;
    private final UserRepository userRepository;

    public RecipeController(RecipeRepository recipeRepository,
                            ImageStorageService imageStorageService,
                            UserRepository userRepository) {
        this.recipeRepository = recipeRepository;
        this.imageStorageService = imageStorageService;
        this.userRepository = userRepository;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    public ResponseEntity<Recipe> createRecipe(
            @RequestParam("title") String title,
            @RequestParam("category") String category,
            @RequestParam("ingredients") String ingredients,
            @RequestParam("instructions") String instructions,
            @RequestParam(value = "image", required = false) MultipartFile imageFile,
            Authentication authentication
    ) throws IOException {
        Recipe recipe = new Recipe();
        String username = authentication.getName();

        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Uživatel nenalezen"));

        recipe.setAuthor(author);
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
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    public ResponseEntity<Recipe> updateRecipe(
            @PathVariable Long id,
            @RequestParam("title") String title,
            @RequestParam("category") String category,
            @RequestParam("ingredients") String ingredients,
            @RequestParam("instructions") String instructions,
            @RequestParam(value = "image", required = false) MultipartFile imageFile,
            Authentication authentication
    ) throws IOException {

        Optional<Recipe> recipeOpt = recipeRepository.findById(id);
        if (recipeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Recipe recipe = recipeOpt.get();

        String currentUsername = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        boolean isAuthor = recipe.getAuthor() != null &&
                recipe.getAuthor().getUsername().equals(currentUsername);

        if (!isAdmin && !isAuthor) {
            return ResponseEntity.status(403).build();
        }

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
    public List<RecipeResponse> getRecipesByCategory(
            @RequestParam(required = false) String category,
            Authentication authentication
    ) {
        User currentUser = getCurrentUserOrNull(authentication);

        List<Recipe> recipes;

        if (category == null || category.isEmpty()) {
            recipes = recipeRepository.findAll();
        } else {
            recipes = recipeRepository.findByCategory(category);
        }

        return recipes.stream()
                .map(recipe -> new RecipeResponse(recipe, currentUser))
                .toList();
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    public List<RecipeResponse> getMyRecipes(Authentication authentication) {
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Uživatel nenalezen"));

        return recipeRepository.findByAuthor_Id(user.getId())
                .stream()
                .map(recipe -> new RecipeResponse(recipe, user))
                .toList();
    }

    @PostMapping("/{id}/favorite")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    public ResponseEntity<String> toggleFavorite(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Uživatel nenalezen"));

        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recept nenalezen"));

        if (user.getFavoriteRecipes().contains(recipe)) {
            user.getFavoriteRecipes().remove(recipe);
            userRepository.save(user);
            return ResponseEntity.ok("Recept odebrán z oblíbených.");
        } else {
            user.getFavoriteRecipes().add(recipe);
            userRepository.save(user);
            return ResponseEntity.ok("Recept přidán do oblíbených.");
        }
    }

    @GetMapping("/favorites")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    public List<RecipeResponse> getFavoriteRecipes(Authentication authentication) {
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Uživatel nenalezen"));

        return recipeRepository.findFavoriteRecipesByUser(user)
                .stream()
                .map(recipe -> new RecipeResponse(recipe, user))
                .toList();
    }

    @PreAuthorize("permitAll()")
    @GetMapping("/{id}")
    public Optional<Recipe> getRecipeById(@PathVariable Long id) {
        return recipeRepository.findById(id);
    }

    @GetMapping("/categories")
    public List<String> getAllCategories() {
        return recipeRepository.findDistinctCategories();
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRecipe(@PathVariable Long id,
                                          Authentication authentication) throws IOException {

        Optional<Recipe> recipeOpt = recipeRepository.findById(id);
        if (recipeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Recipe recipe = recipeOpt.get();

        String currentUsername = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        boolean isAuthor = recipe.getAuthor() != null &&
                recipe.getAuthor().getUsername().equals(currentUsername);

        if (!isAdmin && !isAuthor) {
            return ResponseEntity.status(403).body("Nemáš oprávnění smazat tento recept.");
        }

        for (User user : recipe.getLikedByUsers()) {
            user.getFavoriteRecipes().remove(recipe);
        }

        recipe.getLikedByUsers().clear();

        imageStorageService.delete(recipe.getImageUrl());

        recipeRepository.deleteById(id);
        return ResponseEntity.ok("Recept byl smazán.");
    }
    private User getCurrentUserOrNull(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String username = authentication.getName();

        return userRepository.findByUsername(username).orElse(null);
    }
}

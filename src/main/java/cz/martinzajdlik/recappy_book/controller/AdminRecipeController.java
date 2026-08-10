package cz.martinzajdlik.recappy_book.controller;

import cz.martinzajdlik.recappy_book.dto.RecipeResponse;
import cz.martinzajdlik.recappy_book.model.RecipeStatus;
import cz.martinzajdlik.recappy_book.repository.RecipeRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/recepty")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRecipeController {

    private final RecipeRepository recipeRepository;

    public AdminRecipeController(RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    // Všechny recepty bez ohledu na stav – pro obrazovku "Správa receptů"
    @GetMapping
    public List<RecipeResponse> getAllRecipesForAdmin() {
        return recipeRepository.findAll().stream()
                .map(recipe -> new RecipeResponse(recipe, null))
                .toList();
    }

    // Recepty čekající na schválení
    @GetMapping("/pending")
    public List<RecipeResponse> getPendingRecipes() {
        return recipeRepository.findByStatus(RecipeStatus.PENDING).stream()
                .map(recipe -> new RecipeResponse(recipe, null))
                .toList();
    }

    // Počet receptů čekajících na schválení – pro badge v adminově menu
    @GetMapping("/pending/count")
    public long getPendingRecipesCount() {
        return recipeRepository.countByStatus(RecipeStatus.PENDING);
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<String> approveRecipe(@PathVariable Long id) {
        return recipeRepository.findById(id)
                .map(r -> {
                    r.setStatus(RecipeStatus.APPROVED);
                    recipeRepository.save(r);
                    return ResponseEntity.ok("Recept byl schválen.");
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<String> rejectRecipe(@PathVariable Long id) {
        return recipeRepository.findById(id)
                .map(r -> {
                    r.setStatus(RecipeStatus.REJECTED);
                    recipeRepository.save(r);
                    return ResponseEntity.ok("Recept byl zamítnut.");
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

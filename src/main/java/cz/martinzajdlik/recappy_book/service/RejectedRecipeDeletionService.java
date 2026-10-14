package cz.martinzajdlik.recappy_book.service;

import cz.martinzajdlik.recappy_book.model.Recipe;
import cz.martinzajdlik.recappy_book.model.RecipeStatus;
import cz.martinzajdlik.recappy_book.model.User;
import cz.martinzajdlik.recappy_book.repository.RecipeReportRepository;
import cz.martinzajdlik.recappy_book.repository.RecipeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Smaže JEDEN zamítnutý recept, jehož lhůta vypršela – každý recept ve vlastní
 * transakci, takže chyba u jednoho neovlivní ostatní.
 *
 * Bezpečnostní pojistka: recept se těsně před smazáním znovu načte z DB a
 * všechny podmínky se ověří ještě jednou (stav REJECTED, známé datum zamítnutí,
 * starší než cutoff). Když cokoli z toho neplatí (autor ho mezitím upravil,
 * admin ho schválil…), nic se nesmaže.
 */
@Service
public class RejectedRecipeDeletionService {

    private final RecipeRepository recipeRepository;
    private final RecipeReportRepository recipeReportRepository;

    public RejectedRecipeDeletionService(RecipeRepository recipeRepository,
                                         RecipeReportRepository recipeReportRepository) {
        this.recipeRepository = recipeRepository;
        this.recipeReportRepository = recipeReportRepository;
    }

    /** Výsledek smazání – URL obrázku se maže z Cloudinary až po úspěšném commitu. */
    public record DeletedRecipe(Long id, String title, String imageUrl) {}

    @Transactional
    public Optional<DeletedRecipe> deleteIfExpired(Long recipeId, Instant cutoff) {
        Optional<Recipe> recipeOpt = recipeRepository.findById(recipeId);
        if (recipeOpt.isEmpty()) {
            return Optional.empty();
        }

        Recipe recipe = recipeOpt.get();
        boolean stillExpiredRejected = recipe.getStatus() == RecipeStatus.REJECTED
                && recipe.getRejectedAt() != null
                && recipe.getRejectedAt().isBefore(cutoff);

        if (!stillExpiredRejected) {
            return Optional.empty();
        }

        DeletedRecipe result = new DeletedRecipe(recipe.getId(), recipe.getTitle(), recipe.getImageUrl());

        // Stejný úklid jako v RecipeController.deleteRecipe – jinak by smazání
        // zablokovaly cizí klíče (oblíbené, nahlášení).
        for (User user : recipe.getLikedByUsers()) {
            user.getFavoriteRecipes().remove(recipe);
        }
        recipe.getLikedByUsers().clear();

        recipeReportRepository.deleteByRecipe_Id(recipe.getId());
        recipeRepository.delete(recipe);

        return Optional.of(result);
    }
}

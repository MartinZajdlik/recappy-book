package cz.martinzajdlik.recappy_book.repository;

import cz.martinzajdlik.recappy_book.model.Recipe;
import cz.martinzajdlik.recappy_book.model.RecipeReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecipeReportRepository extends JpaRepository<RecipeReport, Long> {

    boolean existsByRecipe_IdAndReportedBy_IdAndResolvedFalse(Long recipeId, Long reporterId);

    List<RecipeReport> findByRecipe_IdAndResolvedFalse(Long recipeId);

    long countByRecipe_IdAndResolvedFalse(Long recipeId);

    // Recepty, které mají alespoň jedno nevyřízené nahlášení – pro obrazovku
    // "Nahlášené recepty" v adminu.
    @Query("SELECT DISTINCT rr.recipe FROM RecipeReport rr WHERE rr.resolved = false ORDER BY rr.recipe.id DESC")
    List<Recipe> findDistinctReportedRecipes();

    void deleteByRecipe_Id(Long recipeId);

    void deleteByReportedBy_Id(Long userId);
}

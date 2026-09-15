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
    // "Nahlášené recepty" v adminu. Vracíme jen id (ne rovnou entitu) –
    // "SELECT DISTINCT entita ... ORDER BY entita.pole" umí u Postgresu
    // spadnout na "ORDER BY expressions must appear in select list",
    // takže řazení a načtení entit necháváme na volajícím.
    @Query("SELECT DISTINCT rr.recipe.id FROM RecipeReport rr WHERE rr.resolved = false")
    List<Long> findDistinctReportedRecipeIds();

    void deleteByRecipe_Id(Long recipeId);

    void deleteByReportedBy_Id(Long userId);
}

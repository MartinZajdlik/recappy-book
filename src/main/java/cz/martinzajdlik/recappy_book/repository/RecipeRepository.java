package cz.martinzajdlik.recappy_book.repository;
import cz.martinzajdlik.recappy_book.model.User;
import cz.martinzajdlik.recappy_book.model.Recipe;
import cz.martinzajdlik.recappy_book.model.RecipeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import java.util.List;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    @Query("SELECT DISTINCT r.category FROM Recipe r WHERE r.status = :status")
    List<String> findDistinctCategoriesByStatus(@Param("status") RecipeStatus status);

    List<Recipe> findByStatus(RecipeStatus status);

    long countByStatus(RecipeStatus status);

    List<Recipe> findByCategoryAndStatus(String category, RecipeStatus status);

    List<Recipe> findByAuthor_Id(Long authorId);

    void deleteByAuthor_Id(Long authorId);

    @Query("SELECT r FROM Recipe r JOIN r.likedByUsers u WHERE u = :user")
    List<Recipe> findFavoriteRecipesByUser(@Param("user") User user);

    // Kandidáti na automatické smazání: jen ZAMÍTNUTÉ recepty se známým datem
    // zamítnutí starším než cutoff. Recepty s rejectedAt = NULL sem nikdy nespadnou.
    @Query("SELECT r.id FROM Recipe r WHERE r.status = :status " +
           "AND r.rejectedAt IS NOT NULL AND r.rejectedAt < :cutoff ORDER BY r.rejectedAt")
    List<Long> findIdsByStatusAndRejectedAtBefore(@Param("status") RecipeStatus status,
                                                  @Param("cutoff") Instant cutoff);

    // Zamítnuté recepty z doby před zavedením rejectedAt nemají datum – dostanou
    // ho teď, takže jejich 30denní lhůta začne běžet až od nasazení, ne zpětně.
    @Modifying
    @Transactional
    @Query("UPDATE Recipe r SET r.rejectedAt = :now WHERE r.status = :status AND r.rejectedAt IS NULL")
    int fillMissingRejectedAt(@Param("status") RecipeStatus status, @Param("now") Instant now);



}

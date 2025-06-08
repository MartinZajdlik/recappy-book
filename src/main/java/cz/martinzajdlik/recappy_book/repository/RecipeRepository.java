package cz.martinzajdlik.recappy_book.repository;

import cz.martinzajdlik.recappy_book.model.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    @Query("SELECT DISTINCT r.category FROM Recipe r")
    List<String> findDistinctCategories();

    List<Recipe> findByCategory(String category);



}

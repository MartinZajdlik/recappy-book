package cz.martinzajdlik.recappy_book.repository;

import cz.martinzajdlik.recappy_book.model.MealPlanEntry;
import cz.martinzajdlik.recappy_book.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MealPlanRepository extends JpaRepository<MealPlanEntry, Long> {
    Optional<MealPlanEntry> findByUserAndDayOfWeek(User user, int dayOfWeek);

    List<MealPlanEntry> findByUser(User user);
}

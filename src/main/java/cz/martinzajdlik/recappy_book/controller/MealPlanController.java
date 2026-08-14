package cz.martinzajdlik.recappy_book.controller;

import cz.martinzajdlik.recappy_book.dto.MealPlanEntryResponse;
import cz.martinzajdlik.recappy_book.dto.MealPlanUpdateRequest;
import cz.martinzajdlik.recappy_book.model.MealPlanEntry;
import cz.martinzajdlik.recappy_book.model.User;
import cz.martinzajdlik.recappy_book.repository.MealPlanRepository;
import cz.martinzajdlik.recappy_book.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/jidelnicek")
public class MealPlanController {

    private final MealPlanRepository mealPlanRepository;
    private final UserRepository userRepository;

    public MealPlanController(MealPlanRepository mealPlanRepository, UserRepository userRepository) {
        this.mealPlanRepository = mealPlanRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    public List<MealPlanEntryResponse> getMealPlan(Authentication authentication) {
        User user = getCurrentUser(authentication);

        return IntStream.rangeClosed(1, 7)
                .mapToObj(day -> mealPlanRepository.findByUserAndDayOfWeek(user, day)
                        .map(MealPlanEntryResponse::new)
                        .orElseGet(() -> new MealPlanEntryResponse(day)))
                .toList();
    }

    @PutMapping("/{denVTydnu}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    public ResponseEntity<?> updateMealPlanDay(
            @PathVariable int denVTydnu,
            @RequestBody MealPlanUpdateRequest dto,
            Authentication authentication
    ) {
        if (denVTydnu < 1 || denVTydnu > 7) {
            return ResponseEntity.badRequest().body("Den v týdnu musí být v rozsahu 1-7.");
        }

        User user = getCurrentUser(authentication);

        MealPlanEntry entry = mealPlanRepository.findByUserAndDayOfWeek(user, denVTydnu)
                .orElseGet(() -> {
                    MealPlanEntry newEntry = new MealPlanEntry();
                    newEntry.setUser(user);
                    newEntry.setDayOfWeek(denVTydnu);
                    return newEntry;
                });

        entry.setBreakfast(dto.breakfast());
        entry.setSnack1(dto.snack1());
        entry.setLunch(dto.lunch());
        entry.setSnack2(dto.snack2());
        entry.setDinner(dto.dinner());

        MealPlanEntry saved = mealPlanRepository.save(entry);
        return ResponseEntity.ok(new MealPlanEntryResponse(saved));
    }

    private User getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Uživatel nenalezen"));
    }
}

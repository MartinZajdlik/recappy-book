package cz.martinzajdlik.recappy_book.controller;

import cz.martinzajdlik.recappy_book.dto.RecipeReportSummary;
import cz.martinzajdlik.recappy_book.model.Recipe;
import cz.martinzajdlik.recappy_book.model.RecipeReport;
import cz.martinzajdlik.recappy_book.repository.RecipeReportRepository;
import cz.martinzajdlik.recappy_book.repository.RecipeRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Obrazovka "Nahlášené recepty" pro admina. Recepty se do fronty dostávají
 * přes RecipeController.reportRecipe (uživatelské nahlášení nevhodného
 * obsahu). Admin je buď ponechá (nahlášení se označí za vyřízené, recept
 * zůstává veřejný), nebo smaže – k mazání se použije stávající
 * DELETE /recepty/{id}, které samo uklidí i související nahlášení.
 */
@RestController
@RequestMapping("/admin/recepty/reports")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRecipeReportController {

    private final RecipeReportRepository recipeReportRepository;
    private final RecipeRepository recipeRepository;

    public AdminRecipeReportController(RecipeReportRepository recipeReportRepository,
                                       RecipeRepository recipeRepository) {
        this.recipeReportRepository = recipeReportRepository;
        this.recipeRepository = recipeRepository;
    }

    @GetMapping
    public List<RecipeReportSummary> getReportedRecipes() {
        List<Long> reportedIds = recipeReportRepository.findDistinctReportedRecipeIds();
        if (reportedIds.isEmpty()) {
            return List.of();
        }

        // Kdo a kdy nahlásil – seskupeno podle receptu, nejnovější nahlášení první.
        Map<Long, List<RecipeReportSummary.Reporter>> reportersByRecipe =
                recipeReportRepository.findUnresolvedWithReporterByRecipeIds(reportedIds).stream()
                        .sorted(Comparator.comparing(RecipeReport::getCreatedAt).reversed())
                        .collect(Collectors.groupingBy(
                                r -> r.getRecipe().getId(),
                                Collectors.mapping(
                                        r -> new RecipeReportSummary.Reporter(
                                                r.getReportedBy().getUsername(),
                                                r.getCreatedAt().toEpochMilli()),
                                        Collectors.toList())));

        return recipeRepository.findAllById(reportedIds).stream()
                .sorted(Comparator.comparing(Recipe::getId).reversed())
                .map(recipe -> new RecipeReportSummary(
                        recipe,
                        recipeReportRepository.countByRecipe_IdAndResolvedFalse(recipe.getId()),
                        reportersByRecipe.getOrDefault(recipe.getId(), List.of())))
                .toList();
    }

    // Počet receptů s nevyřízeným nahlášením – pro badge v adminově menu.
    @GetMapping("/count")
    public long getReportedRecipesCount() {
        return recipeReportRepository.findDistinctReportedRecipeIds().size();
    }

    // "Ponechat" – nahlášení tohoto receptu se označí za vyřízené, recept zůstává veřejný.
    @PatchMapping("/{recipeId}/dismiss")
    public ResponseEntity<String> dismissReports(@PathVariable Long recipeId) {
        List<RecipeReport> reports = recipeReportRepository.findByRecipe_IdAndResolvedFalse(recipeId);
        if (reports.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        reports.forEach(r -> r.setResolved(true));
        recipeReportRepository.saveAll(reports);
        return ResponseEntity.ok("Nahlášení bylo vyřízeno, recept zůstává.");
    }
}
